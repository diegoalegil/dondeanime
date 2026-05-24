package com.dondeanime.backend.anime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dondeanime.backend.config.SecurityConfig;
import com.dondeanime.backend.affiliate.AffiliateLinkService;
import com.dondeanime.backend.provider.ProviderDto;
import com.dondeanime.backend.provider.ProviderSyncService;
import com.dondeanime.backend.provider.WatchProvider;
import com.dondeanime.backend.provider.WatchProviderRepository;

/**
 * Tests del AnimeController con MockMvc. Carga solo el slice de
 * Spring MVC y mockea todos los beans que el controller inyecta.
 *
 * No toca BD ni APIs externas: las queries y los services están
 * sustituidos por Mockito.
 */
@WebMvcTest(AnimeController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "admin.username=admin",
        "admin.password=secret",
        "admin.cors.allowed-origins=http://localhost:4321"
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
    private TrailerSyncService trailerSyncService;

    @MockitoBean
    private WatchProviderRepository providerRepository;

    @MockitoBean
    private AnimeOverrideService overrideService;

    @MockitoBean
    private AffiliateLinkService affiliateLinkService;

    @Test
    void getAllReturnsListOfSummaries() throws Exception {
        Anime a = makeAnime("attack-on-titan", "Attack on Titan");
        when(animeRepository.findAll()).thenReturn(List.of(a));

        mvc.perform(get("/api/anime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("attack-on-titan"))
                .andExpect(jsonPath("$[0].titleEnglish").value("Attack on Titan"))
                // El DTO no debe exponer id interno ni tmdbId ni syncedAt.
                .andExpect(jsonPath("$[0].id").doesNotExist())
                .andExpect(jsonPath("$[0].tmdbId").doesNotExist())
                .andExpect(jsonPath("$[0].syncedAt").doesNotExist());
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
