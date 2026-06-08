package com.dondeanime.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;
import com.dondeanime.backend.provider.WatchProvider;
import com.dondeanime.backend.provider.WatchProviderRepository;

import jakarta.persistence.EntityManagerFactory;

@ActiveProfiles("prod")
@SpringBootTest(properties = {
        "tmdb.api-key=test",
        "admin.username=admin",
        "admin.password=test-admin-password",
        "alerts.jwt-secret=test-jwt-secret",
        "resend.enabled=false",
        "resend.api-key=test-resend-key",
        "plausible.enabled=false",
        "scheduling.enabled=false",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@AutoConfigureMockMvc
class EndpointQueryCountIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("dondeanime")
            .withUsername("dondeanime_user")
            .withPassword("test");

    static {
        POSTGRES.start();
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AnimeRepository animeRepository;

    @Autowired
    private WatchProviderRepository providerRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @AfterAll
    static void stopPostgres() {
        POSTGRES.stop();
    }

    @BeforeEach
    void seedCatalog() {
        providerRepository.deleteAll();
        animeRepository.deleteAll();

        List<Anime> anime = new ArrayList<>();
        for (long i = 1; i <= 6; i++) {
            anime.add(anime(i));
        }

        List<Anime> saved = animeRepository.saveAll(anime);
        animeRepository.flush();

        List<WatchProvider> providers = saved.stream()
                .map(EndpointQueryCountIntegrationTest::crunchyrollEs)
                .toList();

        providerRepository.saveAll(providers);
        providerRepository.flush();
    }

    @Test
    void providerCountryEndpointDoesNotLoadGenresWithNPlusOne() throws Exception {
        assertEndpointQueryCount("/api/providers/crunchyroll/ES", 2);
    }

    @Test
    void countryAnimeEndpointDoesNotLoadGenresWithNPlusOne() throws Exception {
        assertEndpointQueryCount("/api/providers/country/ES/anime", 2);
    }

    @Test
    void searchEndpointDoesNotLoadGenresWithNPlusOne() throws Exception {
        assertEndpointQueryCount("/api/search?q=Anime&limit=6", 2);
    }

    @Test
    void animeListEndpointDoesNotLoadGenresWithNPlusOne() throws Exception {
        assertEndpointQueryCount("/api/anime", 2);
    }

    @Test
    void seasonEndpointDoesNotLoadGenresWithNPlusOne() throws Exception {
        assertEndpointQueryCount("/api/seasons/2024/spring", 2);
    }

    @Test
    void genreEndpointDoesNotLoadGenresWithNPlusOne() throws Exception {
        assertEndpointQueryCount("/api/genres/action", 2);
    }

    @Test
    void providerAggregationByCountryUsesSingleQuery() throws Exception {
        assertEndpointQueryCount("/api/providers?country=ES", 1);
    }

    @Test
    void publicEndpointQueriesHaveUsableIndexes() {
        assertPlanUsesIndex("""
                select anime_id
                from watch_provider
                where country_code = 'ES'
                  and lower(replace(provider_name, ' ', '-')) = 'crunchyroll'
                """, "idx_watch_provider_slug_country_anime");

        assertPlanUsesIndex("""
                select id
                from anime
                where season_year = 2024
                  and season = 'SPRING'
                order by popularity desc nulls last
                """, "idx_anime_season_year");

        assertPlanUsesIndex("""
                select id
                from anime
                order by popularity desc nulls last
                limit 5
                """, "idx_anime_popularity_desc");

        assertPlanUsesIndex("""
                select anime_id
                from anime_genre
                where lower(replace(genre, ' ', '-')) = 'action'
                """, "idx_anime_genre_slug_anime");
    }

    private void assertEndpointQueryCount(String path, long maxStatements) throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        mvc.perform(get(path))
                .andExpect(status().isOk());

        assertThat(statistics.getPrepareStatementCount())
                .as("SQL statements for %s", path)
                .isLessThanOrEqualTo(maxStatements);
    }

    private void assertPlanUsesIndex(String sql, String indexName) {
        jdbc.execute("set enable_seqscan = off");
        try {
            String plan = String.join("\n", jdbc.queryForList("""
                    explain (analyze, costs off, timing off, summary off)
                    """ + sql, String.class));

            assertThat(plan)
                    .as("query plan should use %s%n%s", indexName, plan)
                    .contains(indexName);
        } finally {
            jdbc.execute("reset enable_seqscan");
        }
    }

    private static Anime anime(long index) {
        Anime anime = new Anime();
        anime.setAnilistId(10_000L + index);
        anime.setSlug("anime-" + index);
        anime.setTitleEnglish("Anime " + index);
        anime.setTitleRomaji("Anime Romaji " + index);
        anime.setFormat("TV");
        anime.setStatus("FINISHED");
        anime.setEpisodes(12);
        anime.setStartYear(2024);
        anime.setAverageScore(80);
        anime.setPopularity((int) (10_000 - index));
        anime.setCoverImage("https://example.com/anime-" + index + ".jpg");
        anime.setSeason("SPRING");
        anime.setSeasonYear(2024);
        anime.setGenres(Set.of("Action", "Adventure", "Slice of Life"));
        return anime;
    }

    private static WatchProvider crunchyrollEs(Anime anime) {
        WatchProvider provider = new WatchProvider();
        provider.setAnimeId(anime.getId());
        provider.setCountryCode("ES");
        provider.setProviderName("Crunchyroll");
        provider.setProviderType("FLATRATE");
        provider.setTmdbProviderId(283);
        provider.setLogoUrl("https://example.com/crunchyroll.jpg");
        provider.setUpdatedAt(Instant.now());
        return provider;
    }
}
