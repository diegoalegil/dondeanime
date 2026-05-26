package com.dondeanime.backend.push;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

import com.dondeanime.backend.admin.auth.AdminJwtService;
import com.dondeanime.backend.config.SecurityConfig;

@WebMvcTest(NotificationAdminController.class)
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
class NotificationAdminControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private NotificationDashboardService notificationDashboardService;

    @Autowired
    private AdminJwtService adminJwtService;

    @Test
    void statsRequiresAuth() throws Exception {
        mvc.perform(get("/api/admin/notifications/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void statsReturnsAggregatedMetricsWithCredentials() throws Exception {
        when(notificationDashboardService.stats()).thenReturn(stats());

        mvc.perform(get("/api/admin/notifications/stats")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeSubscriptions").value(1))
                .andExpect(jsonPath("$.alertsSentLast24Hours").value(3))
                .andExpect(jsonPath("$.deliveryRatePercent").value(75.0))
                .andExpect(jsonPath("$.subscriptions[0].userEmail").value("diego@example.com"));
    }

    @Test
    void testPushRequiresAuth() throws Exception {
        mvc.perform(post("/api/admin/notifications/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subscriptionId\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testPushWithCredentials() throws Exception {
        when(notificationDashboardService.sendTestPush(anyLong()))
                .thenReturn(new NotificationTestResponse(true, 201, "Push test enviado."));

        mvc.perform(post("/api/admin/notifications/test")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subscriptionId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(true))
                .andExpect(jsonPath("$.statusCode").value(201));

        verify(notificationDashboardService).sendTestPush(1L);
    }

    private static NotificationStatsDto stats() {
        return new NotificationStatsDto(
                1,
                3,
                4,
                3,
                1,
                75.0,
                true,
                List.of(new PushSubscriptionAdminDto(
                        1L,
                        "diego@example.com",
                        "ES",
                        Instant.parse("2026-05-25T00:00:00Z"),
                        3,
                        1,
                        201,
                        Instant.parse("2026-05-25T01:00:00Z"),
                null)));
    }

    private String bearerToken() {
        return "Bearer " + adminJwtService.createAdminSession().token();
    }
}
