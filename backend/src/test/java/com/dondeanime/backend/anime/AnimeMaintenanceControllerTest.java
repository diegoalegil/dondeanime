package com.dondeanime.backend.anime;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dondeanime.backend.admin.auth.AdminJwtService;
import com.dondeanime.backend.config.SecurityConfig;
import com.dondeanime.backend.provider.ProviderSyncService;

@WebMvcTest(AnimeMaintenanceController.class)
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
class AnimeMaintenanceControllerTest {

    @Autowired
    private MockMvc mvc;

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
        mvc.perform(post("/api/anime/sync")
                        .param("count", String.valueOf(AnimeSyncService.MAX_POPULAR_SYNC_COUNT + 1)))
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
}
