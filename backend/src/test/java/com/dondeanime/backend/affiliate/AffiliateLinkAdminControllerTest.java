package com.dondeanime.backend.affiliate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dondeanime.backend.admin.auth.AdminJwtService;
import com.dondeanime.backend.anime.RecommendationClickDto;
import com.dondeanime.backend.anime.RecommendationTrackRequest;
import com.dondeanime.backend.anime.RecommendationTrackingController;
import com.dondeanime.backend.anime.RecommendationTrackingService;
import com.dondeanime.backend.config.SecurityConfig;
import com.dondeanime.backend.trakt.TraktDashboardMetricsDto;

@WebMvcTest({
        AffiliateLinkAdminController.class,
        AffiliateTrackingController.class,
        AffiliateDashboardController.class,
        RecommendationTrackingController.class
})
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
class AffiliateLinkAdminControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AffiliateLinkService affiliateLinkService;

    @Autowired
    private AdminJwtService adminJwtService;

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
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].providerSlug").value("crunchyroll"))
                .andExpect(jsonPath("$[0].countryCode").value("ES"));
    }

    @Test
    void postAffiliateLinkWithCredentials() throws Exception {
        when(affiliateLinkService.saveLink(any(AffiliateLinkRequest.class))).thenReturn(dto());

        mvc.perform(post("/api/admin/affiliate-links")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerSlug":"crunchyroll","country":"ES","affiliateUrl":"https://example.com/cr","active":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affiliateUrl").value("https://example.com/cr"));
    }

    @Test
    void bulkImportWithCredentials() throws Exception {
        when(affiliateLinkService.bulkImport(any(String.class)))
                .thenReturn(new AffiliateBulkImportResult(1, List.of(dto())));

        mvc.perform(post("/api/admin/affiliate-links/bulk")
                        .header("Authorization", bearerToken())
                        .contentType("text/csv")
                        .content("""
                                provider_slug,country_code,url,active
                                crunchyroll,ES,https://example.com/cr,true
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.links[0].providerSlug").value("crunchyroll"));
    }

    @Test
    void bulkImportReturnsValidationErrors() throws Exception {
        when(affiliateLinkService.bulkImport(any(String.class)))
                .thenThrow(new AffiliateBulkImportException(List.of(
                        new AffiliateBulkImportError(2, "provider_slug+country_code no existe en catálogo"))));

        mvc.perform(post("/api/admin/affiliate-links/bulk")
                        .header("Authorization", bearerToken())
                        .contentType("text/csv")
                        .content("""
                                provider_slug,country_code,url,active
                                unknown,ES,https://example.com/cr,true
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].line").value(2));
    }

    @Test
    void deleteAffiliateLinkWithCredentials() throws Exception {
        mvc.perform(delete("/api/admin/affiliate-links/1")
                        .header("Authorization", bearerToken()))
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

    @Test
    void dashboardWithCredentialsReturnsExtendedMetrics() throws Exception {
        when(affiliateLinkService.dashboard()).thenReturn(new AffiliateDashboardDto(
                3L,
                9L,
                List.of(new AffiliateAnimeClicksDto("frieren", 4L)),
                List.of(dto()),
                List.of(new PlausiblePageMetricDto("/anime/frieren", 20L)),
                List.of(new AffiliateDailyClicksDto(LocalDate.of(2026, 5, 25), 2L)),
                List.of(new AffiliatePlatformConversionDto("crunchyroll", 4L, 100L, 0.04)),
                List.of(new AffiliateCountryClicksDto("ES", 5L)),
                List.of(new AvailabilityAnimeChangesDto("frieren", 2L)),
                List.of(new RecommendationClickDto("frieren", "violet-evergarden", 3L)),
                12L,
                6L,
                2L,
                1L,
                List.of(new com.dondeanime.backend.curated.CuratedListMetricDto("anime-para-empezar", 12L)),
                new TraktDashboardMetricsDto(2L, 1L, 4L, 3L)));

        mvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clicksByDay[0].clicks").value(2))
                .andExpect(jsonPath("$.platformConversions[0].conversionRate").value(0.04))
                .andExpect(jsonPath("$.topClickCountries[0].countryCode").value("ES"))
                .andExpect(jsonPath("$.topAvailabilityChanges[0].changes").value(2))
                .andExpect(jsonPath("$.topRecommendationClicks[0].targetAnimeSlug").value("violet-evergarden"))
                .andExpect(jsonPath("$.curatedListViewsLast30Days").value(12))
                .andExpect(jsonPath("$.curatedListPremiumConversionsLast30Days").value(1))
                .andExpect(jsonPath("$.topCuratedLists[0].listSlug").value("anime-para-empezar"))
                .andExpect(jsonPath("$.trakt.connectedAccounts").value(2))
                .andExpect(jsonPath("$.trakt.failedMatchesLast30Days").value(3))
                .andExpect(jsonPath("$.trakt.email").doesNotExist())
                .andExpect(jsonPath("$.trakt.accessTokenCiphertext").doesNotExist());
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

    private String bearerToken() {
        return "Bearer " + adminJwtService.createAdminSession().token();
    }
}
