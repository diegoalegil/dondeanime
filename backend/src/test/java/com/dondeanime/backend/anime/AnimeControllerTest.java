package com.dondeanime.backend.anime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dondeanime.backend.admin.auth.AdminJwtService;
import com.dondeanime.backend.config.SecurityConfig;
import com.dondeanime.backend.affiliate.AffiliateLinkService;
import com.dondeanime.backend.provider.ProviderDto;
import com.dondeanime.backend.provider.ProviderSyncService;
import com.dondeanime.backend.provider.WatchProvider;
import com.dondeanime.backend.provider.WatchProviderRepository;

@WebMvcTest(AnimeController.class)
@Import({
        SecurityConfig.class,
        AdminJwtService.class
})
@TestPropertySource(properties = {
        "admin.username=admin",
        "admin.password=secret",
        "admin.cors.allowed-origins=http://localhost:4321",
        "alerts.jwt-secret=test-jwt-secret"
})
class AnimeControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AnimeRepository animeRepository;

    @MockitoBean
    private AnimeSyncService syncService;

    @MockitoBean
    private AnimeMatchingService matchingService;

    @MockitoBean
    private ProviderSyncService providerSyncService;

    @MockitoBean
    private AnimeDescriptionEnricher descriptionEnricher;

    @MockitoBean
    private TrailerSyncService trailerSyncService;

    @MockitoBean
    private WatchProviderRepository providerRepository;

    @MockitoBean
    private AnimeOverrideService overrideService;

    @MockitoBean
    private AffiliateLinkService affiliateLinkService;

    @MockitoBean
    private RecommendationService recommendationService;

    @Test
    void getAllReturnsListOfSummaries() throws Exception {
        Anime a = makeAnime("attack-on-titan", "Attack on Titan");
        when(animeRepository.findAllWithGenres()).thenReturn(List.of(a));

        mvc.perform(get("/api/anime")
                        .header("X-Request-Id", "req-test"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-test"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("attack-on-titan"))
                .andExpect(jsonPath("$[0].titleEnglish").value("Attack on Titan"))
                .andExpect(jsonPath("$[0].id").doesNotExist())
                .andExpect(jsonPath("$[0].tmdbId").doesNotExist())
                .andExpect(jsonPath("$[0].syncedAt").doesNotExist());
    }

    @Test
    void upcomingReturnsOnlyFullDatesWithinRangeOrderedByStartDate() throws Exception {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Anime later = makeAnime("later", "Later");
        later.setPopularity(999);
        startsOn(later, today.plusDays(6));
        Anime early = makeAnime("early", "Early");
        early.setPopularity(1);
        startsOn(early, today.plusDays(2));
        Anime outside = makeAnime("outside", "Outside");
        startsOn(outside, today.plusDays(8));
        Anime partialDate = makeAnime("partial", "Partial");
        partialDate.setStartYear(today.getYear());
        Anime past = makeAnime("past", "Past");
        startsOn(past, today.minusDays(1));

        when(animeRepository.findAll()).thenReturn(List.of(later, outside, partialDate, early, past));

        mvc.perform(get("/api/anime/upcoming?days=7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].slug").value("early"))
                .andExpect(jsonPath("$[0].startYear").value(today.plusDays(2).getYear()))
                .andExpect(jsonPath("$[0].startMonth").value(today.plusDays(2).getMonthValue()))
                .andExpect(jsonPath("$[0].startDay").value(today.plusDays(2).getDayOfMonth()))
                .andExpect(jsonPath("$[1].slug").value("later"));
    }

    @Test
    void upcomingRejectsInvalidDays() throws Exception {
        mvc.perform(get("/api/anime/upcoming?days=0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBySlugReturnsDetailWithProviders() throws Exception {
        Anime a = makeAnime("attack-on-titan", "Attack on Titan");
        WatchProvider provider = provider();
        when(animeRepository.findBySlugWithCharacters("attack-on-titan")).thenReturn(Optional.of(a));
        when(providerRepository
                .findByAnimeIdOrderByCountryCodeAscProviderTypeAscProviderNameAsc(any()))
                .thenReturn(List.of(provider));
        when(affiliateLinkService.toProviderDto(provider)).thenReturn(ProviderDto.from(provider, "https://example.com"));
        when(overrideService.findSpanishOverrides(a)).thenReturn(List.of());

        mvc.perform(get("/api/anime/attack-on-titan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anime.slug").value("attack-on-titan"))
                .andExpect(jsonPath("$.anime.trailerYoutubeId").value("abc123DEF45"))
                .andExpect(jsonPath("$.watchProvidersByCountry").isMap())
                .andExpect(jsonPath("$.watchProvidersByCountry.ES[0].affiliateUrl").value("https://example.com"));
    }

    @Test
    void getBySlugUnknownReturns404() throws Exception {
        when(animeRepository.findBySlugWithCharacters("inexistente")).thenReturn(Optional.empty());

        mvc.perform(get("/api/anime/inexistente"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSimilarReturnsRecommendationSummaries() throws Exception {
        Anime source = makeAnime("attack-on-titan", "Attack on Titan");
        Anime recommendation = makeAnime("vinland-saga", "Vinland Saga");
        recommendation.setId(2L);
        recommendation.setAnilistId(456L);
        recommendation.setAverageScore(88);

        when(animeRepository.findBySlug("attack-on-titan")).thenReturn(Optional.of(source));
        when(recommendationService.findSimilar(1L, 10)).thenReturn(List.of(recommendation));

        mvc.perform(get("/api/anime/attack-on-titan/similar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("vinland-saga"))
                .andExpect(jsonPath("$[0].averageScore").value(88))
                .andExpect(jsonPath("$[0].id").doesNotExist())
                .andExpect(jsonPath("$[0].tmdbId").doesNotExist())
                .andExpect(jsonPath("$[0].syncedAt").doesNotExist());
    }

    @Test
    void getSimilarUnknownSlugReturns404() throws Exception {
        when(animeRepository.findBySlug("inexistente")).thenReturn(Optional.empty());

        mvc.perform(get("/api/anime/inexistente/similar"))
                .andExpect(status().isNotFound());
    }

    @Test
    void matchAlsoEnrichesSpanishDescriptions() throws Exception {
        when(matchingService.matchAll()).thenReturn(3);
        when(descriptionEnricher.enrichMissingSpanishDescriptions()).thenReturn(2);

        mvc.perform(post("/api/anime/match"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(3))
                .andExpect(jsonPath("$.descriptionsEnriched").value(2));
    }

    @Test
    void syncAcceptsFiveHundredAnime() throws Exception {
        when(syncService.syncPopular(500)).thenReturn(500);

        mvc.perform(post("/api/anime/sync").param("count", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synced").value(500));

        verify(syncService).syncPopular(500);
    }

    @Test
    void syncRejectsCountsAboveSprintLimit() throws Exception {
        mvc.perform(post("/api/anime/sync").param("count", "501"))
                .andExpect(status().isBadRequest());

        verify(syncService, never()).syncPopular(anyInt());
    }

    @Test
    void syncTrailersReturnsProcessedCount() throws Exception {
        when(trailerSyncService.syncAll()).thenReturn(42);

        mvc.perform(post("/api/anime/sync-trailers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(42));
    }

    private static Anime makeAnime(String slug, String titleEnglish) {
        Anime a = new Anime();
        a.setId(1L);
        a.setAnilistId(123L);
        a.setTrailerYoutubeId("abc123DEF45");
        a.setSlug(slug);
        a.setTitleEnglish(titleEnglish);
        a.setFormat("TV");
        a.setStatus("FINISHED");
        return a;
    }

    private static void startsOn(Anime anime, LocalDate date) {
        anime.setStartYear(date.getYear());
        anime.setStartMonth(date.getMonthValue());
        anime.setStartDay(date.getDayOfMonth());
    }

    private static WatchProvider provider() {
        WatchProvider provider = new WatchProvider();
        provider.setAnimeId(1L);
        provider.setCountryCode("ES");
        provider.setProviderName("Crunchyroll");
        provider.setProviderType("FLATRATE");
        provider.setLogoUrl("https://example.com/logo.jpg");
        return provider;
    }
}
