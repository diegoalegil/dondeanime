package com.dondeanime.backend.news;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

/**
 * Descarga y parsea feeds RSS/Atom con ROME.
 *
 * Resiliente a propósito: ante un error de red o un XML malformado registra un
 * WARN y devuelve lista vacía, para que el fallo de una fuente no tumbe toda la
 * ingesta. ROME tolera RSS 0.9x/1.0/2.0 y Atom, así que no asumimos formato.
 */
@Component
public class RssNewsFetcher {

    private static final Logger log = LoggerFactory.getLogger(RssNewsFetcher.class);

    /** Recorte defensivo del texto original; el LLM resume después. */
    private static final int MAX_EXCERPT_LENGTH = 2000;

    /** Tope del cuerpo en memoria: corta feeds gigantes o redirects hostiles. */
    private static final int MAX_FEED_BYTES = 10 * 1024 * 1024;

    private final RestClient restClient;

    public RssNewsFetcher(RestClient.Builder builder) {
        // Cliente propio que SÍ sigue redirecciones: varios feeds (p.ej. ANN)
        // hacen 301 a una URL con parámetros de edición, y feedburner redirige
        // al origen. El cliente compartido usa Redirect.NEVER.
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = builder.requestFactory(factory).build();
    }

    /** Descarga el feed y lo parsea. Lista vacía si algo falla. */
    public List<FetchedNewsItem> fetch(String feedUrl) {
        byte[] xml;
        try {
            // URI.create evita que DefaultUriBuilderFactory trate la URL como
            // plantilla (un '{' en el feed reventaría y re-codificaría %xx).
            // exchange permite acotar el cuerpo en heap en vez de bufferizarlo a ciegas.
            xml = restClient.get()
                    .uri(URI.create(feedUrl))
                    .exchange((request, response) -> {
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            log.warn("[news] feed {} respondio {}", feedUrl, response.getStatusCode());
                            return null;
                        }
                        return readCapped(response.getBody());
                    });
        } catch (Exception e) {
            log.warn("[news] no se pudo descargar feed {}: {}", feedUrl, e.getMessage());
            return List.of();
        }
        if (xml == null || xml.length == 0) {
            log.warn("[news] feed {} devolvio cuerpo vacio o excedio el limite", feedUrl);
            return List.of();
        }
        return parse(xml, feedUrl);
    }

    /** Lee como máximo {@code MAX_FEED_BYTES}; si el feed los supera, lo descarta (null). */
    private byte[] readCapped(InputStream body) throws IOException {
        byte[] data = body.readNBytes(MAX_FEED_BYTES + 1);
        if (data.length > MAX_FEED_BYTES) {
            log.warn("[news] feed excede el limite de {} bytes, descartado", MAX_FEED_BYTES);
            return null;
        }
        return data;
    }

    /**
     * Parsea bytes de un feed. Separado de la descarga para poder testearlo sin
     * red, con un XML de fixture. {@code feedUrl} es la base para resolver enlaces
     * e imágenes relativas (típico en Atom).
     */
    List<FetchedNewsItem> parse(byte[] xml, String feedUrl) {
        SyndFeed feed;
        try (ByteArrayInputStream in = new ByteArrayInputStream(xml)) {
            // XmlReader detecta el charset desde la declaración XML / cabeceras.
            feed = new SyndFeedInput().build(new XmlReader(in));
        } catch (Exception e) {
            log.warn("[news] feed {} no es RSS/Atom valido: {}", feedUrl, e.getMessage());
            return List.of();
        }

        List<FetchedNewsItem> items = new ArrayList<>();
        for (SyndEntry entry : feed.getEntries()) {
            FetchedNewsItem item = toItem(entry, feedUrl);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    private FetchedNewsItem toItem(SyndEntry entry, String feedUrl) {
        // Atom puede dar un link relativo (/news/123): lo absolutizamos contra el
        // feed para que valga como source_url (dedup y "ver en origen").
        String url = absolutize(trimToNull(entry.getLink()), feedUrl);
        String title = trimToNull(entry.getTitle());
        // Sin URL no podemos deduplicar ni enlazar a la fuente; sin titulo no hay nada.
        if (url == null || title == null) {
            return null;
        }
        String excerpt = extractExcerpt(entry);
        String imageUrl = extractImage(entry, url);
        Date date = entry.getPublishedDate() != null ? entry.getPublishedDate() : entry.getUpdatedDate();
        Instant publishedAt = date != null ? date.toInstant() : null;
        return new FetchedNewsItem(title, url, excerpt, imageUrl, publishedAt);
    }

    /**
     * Texto plano del cuerpo. El orden importa: primero des-escapamos entidades
     * HTML (un feed doble-escapado trae {@code &lt;b&gt;} en vez de {@code <b>})
     * y luego Jsoup quita los tags. Jsoup, al ser un parser HTML real, respeta
     * comparaciones tipo "bajó <5%" (un '<' sin letra detrás no abre tag), cosa
     * que el viejo regex se comía.
     */
    private String extractExcerpt(SyndEntry entry) {
        String raw = rawBody(entry);
        if (raw == null) {
            return null;
        }
        String text = Jsoup.parse(HtmlUtils.htmlUnescape(raw)).text().trim();
        if (text.isEmpty()) {
            return null;
        }
        return text.length() > MAX_EXCERPT_LENGTH ? text.substring(0, MAX_EXCERPT_LENGTH) : text;
    }

    private String extractImage(SyndEntry entry, String baseUri) {
        // 1) enclosure de tipo imagen (Crunchyroll expone la miniatura asi).
        for (SyndEnclosure enclosure : entry.getEnclosures()) {
            String type = enclosure.getType();
            String url = trimToNull(enclosure.getUrl());
            if (url != null && type != null && type.toLowerCase().startsWith("image")) {
                return url;
            }
        }
        // 2) primer <img> "real" del HTML: resolvemos relativas contra baseUri y
        // saltamos pixeles de tracking 1x1 (feedburner mete uno al principio).
        String html = rawBody(entry);
        if (html != null) {
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
        }
        return null;
    }

    private boolean isTrackingPixel(Element img) {
        return "1".equals(img.attr("width").trim()) || "1".equals(img.attr("height").trim());
    }

    /** Cuerpo crudo del item: prefiere description, cae a content:encoded. */
    private String rawBody(SyndEntry entry) {
        if (entry.getDescription() != null && entry.getDescription().getValue() != null
                && !entry.getDescription().getValue().isBlank()) {
            return entry.getDescription().getValue();
        }
        for (SyndContent content : entry.getContents()) {
            if (content.getValue() != null && !content.getValue().isBlank()) {
                return content.getValue();
            }
        }
        return null;
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
