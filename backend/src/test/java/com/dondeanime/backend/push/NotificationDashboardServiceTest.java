package com.dondeanime.backend.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.dondeanime.backend.subscription.SubscriptionRepository;

import tools.jackson.databind.ObjectMapper;

class NotificationDashboardServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-25T10:00:00Z");

    private final PushSubscriptionRepository pushSubscriptionRepository = mock(PushSubscriptionRepository.class);
    private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
    private final WebPushService webPushService = mock(WebPushService.class);
    private final NotificationDashboardService service = new NotificationDashboardService(
            pushSubscriptionRepository,
            subscriptionRepository,
            webPushService,
            new ObjectMapper(),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void statsAggregatesSubscriptionsAlertsAndDeliveryRate() {
        PushSubscription first = pushSubscription(1L, "diego@example.com", "ES");
        first.setDeliverySuccessCount(3);
        first.setDeliveryFailureCount(1);
        PushSubscription second = pushSubscription(2L, "ana@example.com", "MX");
        second.setDeliverySuccessCount(1);
        second.setDeliveryFailureCount(0);
        when(pushSubscriptionRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(first, second));
        when(subscriptionRepository.countByNotifiedAtAfter(NOW.minusSeconds(86_400))).thenReturn(7L);
        when(webPushService.isConfigured()).thenReturn(true);

        NotificationStatsDto stats = service.stats();

        assertThat(stats.activeSubscriptions()).isEqualTo(2);
        assertThat(stats.alertsSentLast24Hours()).isEqualTo(7);
        assertThat(stats.deliveryAttempts()).isEqualTo(5);
        assertThat(stats.deliverySuccesses()).isEqualTo(4);
        assertThat(stats.deliveryFailures()).isEqualTo(1);
        assertThat(stats.deliveryRatePercent()).isEqualTo(80.0);
        assertThat(stats.pushConfigured()).isTrue();
        assertThat(stats.subscriptions()).extracting(PushSubscriptionAdminDto::userEmail)
                .containsExactly("diego@example.com", "ana@example.com");
    }

    @Test
    void testPushRecordsAcceptedDelivery() {
        PushSubscription subscription = pushSubscription(5L, "diego@example.com", "ES");
        when(pushSubscriptionRepository.findById(5L)).thenReturn(Optional.of(subscription));
        when(webPushService.send(eq(subscription), anyString())).thenReturn(Optional.of(201));

        NotificationTestResponse response = service.sendTestPush(5L);

        assertThat(response.sent()).isTrue();
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(subscription.getDeliverySuccessCount()).isEqualTo(1);
        assertThat(subscription.getLastDeliveredAt()).isEqualTo(NOW);
        verify(pushSubscriptionRepository).save(subscription);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(webPushService).send(eq(subscription), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue())
                .contains("\"title\":\"DondeAnime test push\"")
                .contains("\"url\":\"/admin/notifications\"");
    }

    @Test
    void testPushReportsUnconfiguredWithoutRecordingAttempt() {
        PushSubscription subscription = pushSubscription(5L, "diego@example.com", "ES");
        when(pushSubscriptionRepository.findById(5L)).thenReturn(Optional.of(subscription));
        when(webPushService.send(eq(subscription), anyString())).thenReturn(Optional.empty());

        NotificationTestResponse response = service.sendTestPush(5L);

        assertThat(response.sent()).isFalse();
        assertThat(response.statusCode()).isNull();
        assertThat(subscription.getDeliverySuccessCount()).isNull();
    }

    private static PushSubscription pushSubscription(Long id, String email, String countryIso) {
        PushSubscription subscription = new PushSubscription();
        subscription.setId(id);
        subscription.setUserEmail(email);
        subscription.setCountryIso(countryIso);
        subscription.setEndpoint("https://push.example/" + id);
        subscription.setP256dh("p256dh");
        subscription.setAuth("auth");
        subscription.setCreatedAt(NOW.minusSeconds(id));
        return subscription;
    }
}
