package com.dondeanime.backend.affiliate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dondeanime.backend.anime.RecommendationTrackRequest;
import com.dondeanime.backend.anime.RecommendationTrackingController;
import com.dondeanime.backend.anime.RecommendationTrackingService;
import com.dondeanime.backend.config.SecurityConfig;

@WebMvcTest({
        AffiliateLinkAdminController.class,
        AffiliateTrackingController.class,
        AffiliateDashboardController.class,
        RecommendationTrackingController.class
})
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "admin.username=admin",
        "admin.password=secret",
        "admin.cors.allowed-origins=http://localhost:4321"
})
class AffiliateLinkAdminControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AffiliateLinkService affiliateLinkService;

    @MockitoBean
    private RecommendationTrackingService recommendationTrackingService;

    @Test
    void adminAffiliateLinksRequireAuth() throws Exception {
        mvc.perform(get("/api/admin/affiliate-links"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listAffiliateLinksWithCredentials() throws Exception {
        when(affiliateLinkService.listLinks()).thenReturn(List.of(dto()));

        mvc.perform(get("/api/admin/affiliate-links")
                        .with(httpBasic("admin", "secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].providerSlug").value("crunchyroll"))
                .andExpect(jsonPath("$[0].countryCode").value("ES"));
    }

    @Test
    void postAffiliateLinkWithCredentials() throws Exception {
        when(affiliateLinkService.saveLink(any(AffiliateLinkRequest.class))).thenReturn(dto());

        mvc.perform(post("/api/admin/affiliate-links")
                        .with(httpBasic("admin", "secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerSlug":"crunchyroll","country":"ES","affiliateUrl":"https://example.com/cr","active":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affiliateUrl").value("https://example.com/cr"));
    }

    @Test
    void deleteAffiliateLinkWithCredentials() throws Exception {
        mvc.perform(delete("/api/admin/affiliate-links/1")
                        .with(httpBasic("admin", "secret")))
                .andExpect(status().isNoContent());

        verify(affiliateLinkService).deleteLink(1L);
    }

    @Test
    void affiliateTrackingIsPublic() throws Exception {
        mvc.perform(post("/api/track/affiliate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerSlug":"crunchyroll","country":"ES","animeSlug":"attack-on-titan"}
                                """))
                .andExpect(status().isNoContent());

        verify(affiliateLinkService).trackClick(any(AffiliateTrackRequest.class));
    }

    @Test
    void recommendationTrackingIsPublic() throws Exception {
        mvc.perform(post("/api/track/recommendation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceAnimeSlug":"attack-on-titan","targetAnimeSlug":"vinland-saga"}
                                """))
                .andExpect(status().isNoContent());

        verify(recommendationTrackingService).trackClick(any(RecommendationTrackRequest.class));
    }

    @Test
    void dashboardRequiresAuth() throws Exception {
        mvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    private static AffiliateLinkDto dto() {
        return new AffiliateLinkDto(
                1L,
                "crunchyroll",
                "ES",
                "https://example.com/cr",
                0,
                true,
                Instant.now(),
                Instant.now());
    }
}
