package com.dondeanime.backend.premium;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

    @Test
    void upsertPremiumCreatesNormalizedSubscriber() {
        when(subscriberRepository.findByStripeCustomerId("cus_test_123")).thenReturn(Optional.empty());
        when(subscriberRepository.findByEmail("diego@example.com")).thenReturn(Optional.empty());

        service.upsertPremium(
                " Diego@Example.com ",
                "cus_test_123",
                "premium",
                NOW,
                NOW.plusSeconds(2_592_000),
                NOW);

        ArgumentCaptor<Subscriber> captor = ArgumentCaptor.forClass(Subscriber.class);
        verify(subscriberRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("diego@example.com");
        assertThat(captor.getValue().getStripeCustomerId()).isEqualTo("cus_test_123");
        assertThat(captor.getValue().getPlanTier()).isEqualTo("PREMIUM");
    }

    @Test
    void cancelByStripeCustomerIdSetsExpiry() {
        Subscriber subscriber = subscriber(NOW.minusSeconds(120), null);
        when(subscriberRepository.findByStripeCustomerId("cus_test_123")).thenReturn(Optional.of(subscriber));

        assertThat(service.cancelByStripeCustomerId("cus_test_123", NOW)).isTrue();

        assertThat(subscriber.getExpiresAt()).isEqualTo(NOW);
        verify(subscriberRepository).save(subscriber);
    }

    @Test
    void recordPaymentSucceededUpdatesLastPayment() {
        Subscriber subscriber = subscriber(NOW.minusSeconds(120), null);
        when(subscriberRepository.findByStripeCustomerId("cus_test_123")).thenReturn(Optional.of(subscriber));

        assertThat(service.recordPaymentSucceeded(null, "cus_test_123", NOW)).isTrue();

        assertThat(subscriber.getLastPaymentAt()).isEqualTo(NOW);
        verify(subscriberRepository).save(subscriber);
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
