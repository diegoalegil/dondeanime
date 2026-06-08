package com.dondeanime.backend.anime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.dondeanime.backend.affiliate.AffiliateLinkService;
import com.dondeanime.backend.config.SecurityConfig;
import com.dondeanime.backend.provider.ProviderDto;
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
    private WatchProviderRepository providerRepository;

    @MockitoBean
    private AnimeOverrideService overrideService;

    @MockitoBean
    private AffiliateLinkService affiliateLinkService;

    @MockitoBean
    private RecommendationService recommendationService;

    @Test
    void getAllReturnsListOfSummaries() throws Exception {
        Anime anime = makeAnime("attack-on-titan", "Attack on Titan");
        when(animeRepository.findAllWithGenres()).thenReturn(List.of(anime));

        mvc.perform(get("/api/v1/anime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("attack-on-titan"))
                .andExpect(jsonPath("$[0].titleEnglish").value("Attack on Titan"))
                .andExpect(jsonPath("$[0].episodeDuration").value(24))
                .andExpect(jsonPath("$[0].studio").value("WIT Studio"))
                // El DTO no debe exponer id interno ni tmdbId ni syncedAt.
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

        mvc.perform(get("/api/v1/anime/upcoming?days=7"))
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
        mvc.perform(get("/api/v1/anime/upcoming?days=0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByDurationReturnsMatchingAnime() throws Exception {
        Anime a = makeAnime("attack-on-titan", "Attack on Titan");
        when(animeRepository.findByEpisodeDuration(24)).thenReturn(List.of(a));

        mvc.perform(get("/api/anime/duration/24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("attack-on-titan"))
                .andExpect(jsonPath("$[0].episodeDuration").value(24));
    }

    @Test
    void getByDurationRejectsInvalidMinutes() throws Exception {
        mvc.perform(get("/api/anime/duration/0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByEpisodeCountReturnsMatchingAnime() throws Exception {
        Anime a = makeAnime("attack-on-titan", "Attack on Titan");
        a.setEpisodes(12);
        when(animeRepository.findByEpisodesLessThanOrEqual(12)).thenReturn(List.of(a));

        mvc.perform(get("/api/anime/episodes/less-than/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("attack-on-titan"))
                .andExpect(jsonPath("$[0].episodes").value(12));
    }

    @Test
    void getByEpisodeCountRejectsInvalidLimit() throws Exception {
        mvc.perform(get("/api/anime/episodes/less-than/0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBySlugReturnsDetailWithProviders() throws Exception {
        Anime anime = makeAnime("attack-on-titan", "Attack on Titan");
        WatchProvider provider = provider();
        when(animeRepository.findBySlugWithCharacters("attack-on-titan")).thenReturn(Optional.of(anime));
        when(providerRepository
                .findByAnimeIdOrderByCountryCodeAscProviderTypeAscProviderNameAsc(any()))
                .thenReturn(List.of(provider));
        when(affiliateLinkService.toProviderDtos(List.of(provider)))
                .thenReturn(List.of(ProviderDto.from(provider, "https://example.com")));
        when(overrideService.findSpanishOverrides(anime)).thenReturn(List.of());

        mvc.perform(get("/api/v1/anime/attack-on-titan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anime.slug").value("attack-on-titan"))
                .andExpect(jsonPath("$.anime.trailerYoutubeId").value("abc123DEF45"))
                .andExpect(jsonPath("$.watchProvidersByCountry").isMap())
                .andExpect(jsonPath("$.watchProvidersByCountry.ES[0].affiliateUrl").value("https://example.com"));
    }

    @Test
    void getBySlugUnknownReturns404() throws Exception {
        when(animeRepository.findBySlugWithCharacters("inexistente")).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/anime/inexistente"))
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
        when(recommendationService.findSimilar(1L, 10, null)).thenReturn(List.of(recommendation));

        mvc.perform(get("/api/v1/anime/attack-on-titan/similar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("vinland-saga"))
                .andExpect(jsonPath("$[0].averageScore").value(88))
                .andExpect(jsonPath("$[0].id").doesNotExist())
                .andExpect(jsonPath("$[0].tmdbId").doesNotExist())
                .andExpect(jsonPath("$[0].syncedAt").doesNotExist());
    }

    @Test
    void getSimilarPassesWatchedContextWithoutExposingInternals() throws Exception {
        Anime source = makeAnime("attack-on-titan", "Attack on Titan");
        Anime recommendation = makeAnime("vinland-saga", "Vinland Saga");
        recommendation.setId(2L);
        recommendation.setAnilistId(456L);

        when(animeRepository.findBySlug("attack-on-titan")).thenReturn(Optional.of(source));
        when(recommendationService.findSimilar(
                1L,
                10,
                List.of("death-note", "fullmetal-alchemist-brotherhood")))
                .thenReturn(List.of(recommendation));

        mvc.perform(get("/api/v1/anime/attack-on-titan/similar")
                        .param("watched", "death-note", "fullmetal-alchemist-brotherhood"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("vinland-saga"))
                .andExpect(jsonPath("$[0].id").doesNotExist())
                .andExpect(jsonPath("$[0].tmdbId").doesNotExist())
                .andExpect(jsonPath("$[0].syncedAt").doesNotExist())
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].accessTokenCiphertext").doesNotExist());
    }

    @Test
    void getSimilarUnknownSlugReturns404() throws Exception {
        when(animeRepository.findBySlug("inexistente")).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/anime/inexistente/similar"))
                .andExpect(status().isNotFound());
    }

    @Test
    void versionedApiDoesNotExposeMaintenanceActions() throws Exception {
        mvc.perform(post("/api/v1/anime/sync").param("count", "1"))
                .andExpect(status().isMethodNotAllowed());
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
        a.setEpisodes(24);
        a.setEpisodeDuration(24);
        a.setStudio("WIT Studio");
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
