package com.dondeanime.backend.anime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dondeanime.backend.provider.ProviderSyncService;
import com.dondeanime.backend.provider.WatchProviderRepository;

/**
 * Tests del AnimeController con MockMvc. Carga solo el slice de
 * Spring MVC y mockea todos los beans que el controller inyecta.
 *
 * No toca BD ni APIs externas: las queries y los services están
 * sustituidos por Mockito.
 */
@WebMvcTest(AnimeController.class)
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
    private WatchProviderRepository providerRepository;

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
        when(animeRepository.findBySlug("attack-on-titan")).thenReturn(Optional.of(a));
        when(providerRepository
                .findByAnimeIdOrderByCountryCodeAscProviderTypeAscProviderNameAsc(any()))
                .thenReturn(List.of());

        mvc.perform(get("/api/anime/attack-on-titan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anime.slug").value("attack-on-titan"))
                .andExpect(jsonPath("$.watchProvidersByCountry").isMap());
    }

    @Test
    void getBySlugUnknownReturns404() throws Exception {
        when(animeRepository.findBySlug("inexistente")).thenReturn(Optional.empty());

        mvc.perform(get("/api/anime/inexistente"))
                .andExpect(status().isNotFound());
    }

    private static Anime makeAnime(String slug, String titleEnglish) {
        Anime a = new Anime();
        a.setId(1L);
        a.setAnilistId(123L);
        a.setSlug(slug);
        a.setTitleEnglish(titleEnglish);
        a.setFormat("TV");
        a.setStatus("FINISHED");
        return a;
    }
}
