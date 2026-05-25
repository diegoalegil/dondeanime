package com.dondeanime.backend.premium;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class SubscriberServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-25T10:00:00Z");

    private final SubscriberRepository subscriberRepository = mock(SubscriberRepository.class);
    private final SubscriberService service = new SubscriberService(
            subscriberRepository,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void isPremiumReturnsTrueForActiveSubscriber() {
        Subscriber subscriber = subscriber(NOW.minusSeconds(60), NOW.plusSeconds(60));
        when(subscriberRepository.findByEmail("diego@example.com")).thenReturn(Optional.of(subscriber));

        assertThat(service.isPremium(" Diego@Example.com ")).isTrue();
    }

    @Test
    void isPremiumReturnsFalseForExpiredSubscriber() {
        Subscriber subscriber = subscriber(NOW.minusSeconds(120), NOW.minusSeconds(1));
        when(subscriberRepository.findByEmail("diego@example.com")).thenReturn(Optional.of(subscriber));

        assertThat(service.isPremium("diego@example.com")).isFalse();
    }

    @Test
    void isPremiumReturnsFalseForMissingSubscriber() {
        when(subscriberRepository.findByEmail("diego@example.com")).thenReturn(Optional.empty());

        assertThat(service.isPremium("diego@example.com")).isFalse();
    }

    @Test
    void isPremiumIgnoresBlankEmail() {
        assertThat(service.isPremium(" ")).isFalse();

        verifyNoInteractions(subscriberRepository);
    }

    private static Subscriber subscriber(Instant subscribedAt, Instant expiresAt) {
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail("diego@example.com");
        subscriber.setStripeCustomerId("cus_test_123");
        subscriber.setPlanTier("PREMIUM");
        subscriber.setSubscribedAt(subscribedAt);
        subscriber.setExpiresAt(expiresAt);
        subscriber.setLastPaymentAt(subscribedAt);
        return subscriber;
    }
}
