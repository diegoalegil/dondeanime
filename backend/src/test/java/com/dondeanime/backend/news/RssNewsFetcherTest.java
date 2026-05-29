package com.dondeanime.backend.news;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * Tests del parseo sin red: ejercitan {@link RssNewsFetcher#parse} con fixtures
 * XML. Cubren la limpieza de HTML (que antes hacía un regex frágil), la
 * extracción de imagen y la absolutización de enlaces relativos.
 */
class RssNewsFetcherTest {

    private final RssNewsFetcher fetcher = new RssNewsFetcher(RestClient.builder());

    private List<FetchedNewsItem> parse(String xml, String feedUrl) {
        return fetcher.parse(xml.getBytes(StandardCharsets.UTF_8), feedUrl);
    }

    @Test
    void parsesRss2WithCleanExcerptAndResolvedImage() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0">
                  <channel>
                    <title>Test</title>
                    <link>https://site.com</link>
                    <description>d</description>
                    <item>
                      <title>Anime X anunciado</title>
                      <link>https://site.com/news/anime-x</link>
                      <description><![CDATA[<p>Cuerpo con <img src="/img/a.jpg"> imagen.</p>]]></description>
                      <pubDate>Tue, 27 May 2026 10:00:00 GMT</pubDate>
                    </item>
                  </channel>
                </rss>
                """;
        List<FetchedNewsItem> items = parse(xml, "https://site.com/feed.xml");

        assertThat(items).hasSize(1);
        FetchedNewsItem item = items.get(0);
        assertThat(item.title()).isEqualTo("Anime X anunciado");
        assertThat(item.url()).isEqualTo("https://site.com/news/anime-x");
        assertThat(item.excerpt()).isEqualTo("Cuerpo con imagen.");
        assertThat(item.imageUrl()).isEqualTo("https://site.com/img/a.jpg");
        assertThat(item.publishedAt()).isNotNull();
    }

    @Test
    void excerptKeepsLessThanComparisons() {
        // El viejo regex <[^>]+> se comía "<5% pero reviews >"; Jsoup lo respeta.
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0"><channel><title>t</title><link>https://s.com</link><description>d</description>
                  <item>
                    <title>Nota</title>
                    <link>https://s.com/1</link>
                    <description><![CDATA[la nota bajo <5% pero reviews >8/10]]></description>
                  </item>
                </channel></rss>
                """;
        assertThat(parse(xml, "https://s.com/feed").get(0).excerpt())
                .isEqualTo("la nota bajo <5% pero reviews >8/10");
    }

    @Test
    void excerptStripsDoubleEscapedMarkup() {
        // Feed doble-escapado: el cuerpo trae &lt;b&gt;texto&lt;/b&gt; literal.
        // unescape -> tags reales -> Jsoup los quita -> "texto".
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0"><channel><title>t</title><link>https://s.com</link><description>d</description>
                  <item>
                    <title>Nota</title>
                    <link>https://s.com/2</link>
                    <description><![CDATA[&lt;b&gt;texto&lt;/b&gt;]]></description>
                  </item>
                </channel></rss>
                """;
        assertThat(parse(xml, "https://s.com/feed").get(0).excerpt()).isEqualTo("texto");
    }

    @Test
    void skipsTrackingPixelAndPicksRealImage() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0"><channel><title>t</title><link>https://s.com</link><description>d</description>
                  <item>
                    <title>Nota</title>
                    <link>https://s.com/3</link>
                    <description><![CDATA[<img src="https://track.fb.com/p.gif" width="1" height="1"/><p>hola</p><img src="https://cdn.site.com/real.jpg"/>]]></description>
                  </item>
                </channel></rss>
                """;
        assertThat(parse(xml, "https://s.com/feed").get(0).imageUrl())
                .isEqualTo("https://cdn.site.com/real.jpg");
    }

    @Test
    void prefersEnclosureImageOverBody() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0"><channel><title>t</title><link>https://s.com</link><description>d</description>
                  <item>
                    <title>Nota</title>
                    <link>https://s.com/4</link>
                    <enclosure url="https://cdn.site.com/thumb.jpg" type="image/jpeg" length="1234"/>
                    <description><![CDATA[<img src="https://cdn.site.com/inline.jpg"/>]]></description>
                  </item>
                </channel></rss>
                """;
        assertThat(parse(xml, "https://s.com/feed").get(0).imageUrl())
                .isEqualTo("https://cdn.site.com/thumb.jpg");
    }

    @Test
    void absolutizesRelativeAtomLink() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <feed xmlns="http://www.w3.org/2005/Atom">
                  <title>T</title>
                  <id>urn:x</id>
                  <updated>2026-05-27T10:00:00Z</updated>
                  <entry>
                    <title>Nota Atom</title>
                    <id>urn:1</id>
                    <link href="/news/123"/>
                    <updated>2026-05-27T10:00:00Z</updated>
                    <summary>resumen</summary>
                  </entry>
                </feed>
                """;
        List<FetchedNewsItem> items = parse(xml, "https://atom.example.com/feed.xml");

        assertThat(items).hasSize(1);
        assertThat(items.get(0).url()).isEqualTo("https://atom.example.com/news/123");
    }

    @Test
    void publishedAtNullWhenFeedHasNoDate() {
        // Documenta el comportamiento que S2 resolverá con COALESCE(published_at, fetched_at).
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0"><channel><title>t</title><link>https://s.com</link><description>d</description>
                  <item>
                    <title>Sin fecha</title>
                    <link>https://s.com/5</link>
                    <description>cuerpo</description>
                  </item>
                </channel></rss>
                """;
        assertThat(parse(xml, "https://s.com/feed").get(0).publishedAt()).isNull();
    }

    @Test
    void malformedXmlReturnsEmptyList() {
        assertThat(parse("esto no es xml <<<", "https://s.com/feed")).isEmpty();
        assertThat(fetcher.parse(new byte[0], "https://s.com/feed")).isEmpty();
    }
}
