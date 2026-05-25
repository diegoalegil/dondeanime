package com.dondeanime.backend.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

class PushSubscriptionCleanupServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-25T10:00:00Z");

    private final PushSubscriptionRepository pushSubscriptionRepository = mock(PushSubscriptionRepository.class);
    private final PushSubscriptionCleanupService service = new PushSubscriptionCleanupService(
            pushSubscriptionRepository,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void recordsNonBouncingDeliveryResult() {
        PushSubscription subscription = subscription();

        boolean deleted = service.recordDeliveryResult(subscription, 500);

        assertThat(deleted).isFalse();
        assertThat(subscription.getDeliveryFailureCount()).isEqualTo(1);
        assertThat(subscription.getLastFailedAt()).isEqualTo(NOW);
        verify(pushSubscriptionRepository).save(subscription);
        verify(pushSubscriptionRepository, never()).delete(subscription);
    }

    @Test
    void deletesBouncingDeliveryResult() {
        PushSubscription subscription = subscription();

        boolean deleted = service.recordDeliveryResult(subscription, 410);

        assertThat(deleted).isTrue();
        assertThat(subscription.getDeliveryFailureCount()).isNull();
        verify(pushSubscriptionRepository).delete(subscription);
        verify(pushSubscriptionRepository, never()).save(subscription);
    }

    @Test
    void purgesSubscriptionsInactiveForSixtyDays() {
        Instant cutoff = NOW.minus(60, ChronoUnit.DAYS);
        when(pushSubscriptionRepository.deleteInactiveBefore(cutoff)).thenReturn(3);

        int purged = service.purgeInactiveSubscriptions();

        assertThat(purged).isEqualTo(3);
        verify(pushSubscriptionRepository).deleteInactiveBefore(cutoff);
    }

    private static PushSubscription subscription() {
        PushSubscription subscription = new PushSubscription();
        subscription.setId(7L);
        subscription.setUserEmail("diego@example.com");
        subscription.setEndpoint("https://push.example/7");
        subscription.setP256dh("p256dh");
        subscription.setAuth("auth");
        subscription.setCountryIso("ES");
        subscription.setCreatedAt(NOW);
        return subscription;
    }
}
