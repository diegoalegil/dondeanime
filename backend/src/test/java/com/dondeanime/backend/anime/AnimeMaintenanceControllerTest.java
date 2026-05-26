package com.dondeanime.backend.anime;

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

    @Test
    void matchAlsoEnrichesSpanishDescriptionsOnLegacyMaintenanceEndpoint() throws Exception {
        when(matchingService.matchAll()).thenReturn(3);
        when(descriptionEnricher.enrichMissingSpanishDescriptions()).thenReturn(2);

        mvc.perform(post("/api/anime/match"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(3))
                .andExpect(jsonPath("$.descriptionsEnriched").value(2));
    }
}
