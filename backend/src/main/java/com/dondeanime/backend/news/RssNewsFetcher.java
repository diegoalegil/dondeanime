package com.dondeanime.backend.news;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import io.github.diegoalegil.animefeed.FeedFetchException;
import io.github.diegoalegil.animefeed.FeedParseException;
import io.github.diegoalegil.animefeed.http.FeedClient;
import io.github.diegoalegil.animefeed.http.FeedHttpConfig;
import io.github.diegoalegil.animefeed.model.FeedItem;
import io.github.diegoalegil.animefeed.parse.FeedParserDispatcher;

/**
 * Descarga y parsea feeds RSS/Atom usando la librería {@code anime-feed-parser}
 * (parseo RSS 2.0/Atom + XML seguro anti-XXE + fetch HTTP que sigue redirects).
 *
 * El parseo y la descarga los hace la librería; aquí queda la lógica propia de
 * DondeAnime que la librería no cubre: limpiar el cuerpo a texto plano para el
 * excerpt, extraer la imagen del HTML del cuerpo (saltando píxeles de tracking)
 * cuando el feed no la trae estructurada, y absolutizar enlaces relativos de
 * Atom contra la URL del feed.
 *
 * Resiliente a propósito: ante un error de red o un XML malformado registra un
 * WARN y devuelve lista vacía, para que el fallo de una fuente no tumbe toda la
 * ingesta.
 */
@Component
public class RssNewsFetcher {

    private static final Logger log = LoggerFactory.getLogger(RssNewsFetcher.class);

    /** Recorte defensivo del texto original; el paso editorial resume después. */
    private static final int MAX_EXCERPT_LENGTH = 2000;

    /** Tope del cuerpo: corta feeds gigantes o redirects hostiles. */
    private static final long MAX_FEED_BYTES = 10L * 1024 * 1024;

    private final FeedClient feedClient;
    private final FeedParserDispatcher dispatcher;

    public RssNewsFetcher() {
        // La librería ya sigue redirects (ANN/feedburner hacen 301) y acota el
        // cuerpo en streaming. Subimos el tope a 10MB (default de la lib: 5MB).
        this.feedClient = new FeedClient(FeedHttpConfig.defaults()
                .withMaxResponseBytes(MAX_FEED_BYTES)
                .withRequestTimeout(Duration.ofSeconds(30)));
        this.dispatcher = new FeedParserDispatcher();
    }

    /** Descarga el feed y lo parsea. Lista vacía si algo falla. */
    public List<FetchedNewsItem> fetch(String feedUrl) {
        byte[] xml;
        try {
            xml = feedClient.fetch(feedUrl);
        } catch (FeedFetchException e) {
            log.warn("[news] no se pudo descargar feed {}: {}", feedUrl, e.getMessage());
            return List.of();
        }
        return parse(xml, feedUrl);
    }

    /**
     * Parsea bytes de un feed. Separado de la descarga para poder testearlo sin
     * red, con un XML de fixture. {@code feedUrl} es la base para resolver enlaces
     * e imágenes relativas (típico en Atom).
     */
    List<FetchedNewsItem> parse(byte[] xml, String feedUrl) {
        List<FeedItem> feedItems;
        try {
            feedItems = dispatcher.parse(xml);
        } catch (FeedParseException e) {
            log.warn("[news] feed {} no es RSS/Atom valido: {}", feedUrl, e.getMessage());
            return List.of();
        }

        List<FetchedNewsItem> items = new ArrayList<>();
        for (FeedItem entry : feedItems) {
            FetchedNewsItem item = toItem(entry, feedUrl);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    private FetchedNewsItem toItem(FeedItem entry, String feedUrl) {
        // Atom puede dar un link relativo (/news/123): lo absolutizamos contra el
        // feed para que valga como source_url (dedup y "ver en origen").
        String url = absolutize(trimToNull(entry.link()), feedUrl);
        String title = trimToNull(entry.title());
        // Sin URL no podemos deduplicar ni enlazar a la fuente; sin titulo no hay nada.
        if (url == null || title == null) {
            return null;
        }
        String body = entry.summary();
        String excerpt = extractExcerpt(body);
        // La librería ya extrae la imagen estructurada (media:thumbnail/content,
        // enclosure de imagen); si no la trae, la buscamos en el HTML del cuerpo.
        String imageUrl = entry.imageUrl() != null ? entry.imageUrl() : extractImageFromBody(body, url);
        Instant publishedAt = entry.publishedAt() != null ? entry.publishedAt().toInstant() : null;
        return new FetchedNewsItem(title, url, excerpt, imageUrl, publishedAt);
    }

    /**
     * Texto plano del cuerpo. El orden importa: primero des-escapamos entidades
     * HTML (un feed doble-escapado trae {@code &lt;b&gt;} en vez de {@code <b>})
     * y luego Jsoup quita los tags. Jsoup, al ser un parser HTML real, respeta
     * comparaciones tipo "bajó <5%" (un '<' sin letra detrás no abre tag).
     */
    private String extractExcerpt(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = Jsoup.parse(HtmlUtils.htmlUnescape(raw)).text().trim();
        if (text.isEmpty()) {
            return null;
        }
        return text.length() > MAX_EXCERPT_LENGTH ? text.substring(0, MAX_EXCERPT_LENGTH) : text;
    }

    /**
     * Primer {@code <img>} "real" del HTML del cuerpo: resuelve relativas contra
     * {@code baseUri} y salta píxeles de tracking 1x1 (feedburner mete uno).
     */
    private String extractImageFromBody(String html, String baseUri) {
        if (html == null || html.isBlank()) {
            return null;
        }
        Document doc = Jsoup.parse(html, baseUri != null ? baseUri : "");
        for (Element img : doc.select("img")) {
            String src = img.hasAttr("src") ? img.absUrl("src")
                    : img.hasAttr("data-src") ? img.absUrl("data-src")
                    : "";
            src = trimToNull(src);
            // absUrl deja "" si no pudo absolutizar (relativa sin base, o data:).
            if (src == null || !(src.startsWith("http://") || src.startsWith("https://"))) {
                continue;
            }
            if (isTrackingPixel(img)) {
                continue;
            }
            return src;
        }
        return null;
    }

    private boolean isTrackingPixel(Element img) {
        return "1".equals(img.attr("width").trim()) || "1".equals(img.attr("height").trim());
    }

    /** Link absoluto; resuelve uno relativo contra la base; null si no se puede. */
    private static String absolutize(String link, String base) {
        if (link == null) {
            return null;
        }
        try {
            URI uri = URI.create(link);
            if (uri.isAbsolute()) {
                return link;
            }
            if (base == null) {
                return null;
            }
            return URI.create(base).resolve(uri).toString();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
