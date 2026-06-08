package com.dondeanime.backend.premium;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    void findActiveEntitlementReturnsNormalizedPremiumData() {
        Subscriber subscriber = subscriber(NOW.minusSeconds(60), NOW.plusSeconds(60));
        subscriber.setPlanTier(" premium ");
        when(subscriberRepository.findByEmail("diego@example.com")).thenReturn(Optional.of(subscriber));

        assertThat(service.findActiveEntitlement(" Diego@Example.com "))
                .contains(new PremiumEntitlement(
                        "diego@example.com",
                        "PREMIUM",
                        NOW.plusSeconds(60)));
    }

    @Test
    void findActiveEntitlementRejectsExpiredSubscriber() {
        Subscriber subscriber = subscriber(NOW.minusSeconds(120), NOW.minusSeconds(1));
        when(subscriberRepository.findByEmail("diego@example.com")).thenReturn(Optional.of(subscriber));

        assertThat(service.findActiveEntitlement("diego@example.com")).isEmpty();
    }

    @Test
    void findActiveStripeCustomerIdReturnsCustomerForActivePremiumSubscriber() {
        Subscriber subscriber = subscriber(NOW.minusSeconds(60), NOW.plusSeconds(60));
        when(subscriberRepository.findByEmail("diego@example.com")).thenReturn(Optional.of(subscriber));

        assertThat(service.findActiveStripeCustomerId(" Diego@Example.com "))
                .contains("cus_test_123");
    }

    @Test
    void findActiveStripeCustomerIdIgnoresExpiredSubscriber() {
        Subscriber subscriber = subscriber(NOW.minusSeconds(120), NOW.minusSeconds(1));
        when(subscriberRepository.findByEmail("diego@example.com")).thenReturn(Optional.of(subscriber));

        assertThat(service.findActiveStripeCustomerId("diego@example.com")).isEmpty();
    }

    @Test
    void findActiveStripeCustomerIdIgnoresSubscriberWithoutCustomerId() {
        Subscriber subscriber = subscriber(NOW.minusSeconds(120), null);
        subscriber.setStripeCustomerId(" ");
        when(subscriberRepository.findByEmail("diego@example.com")).thenReturn(Optional.of(subscriber));

        assertThat(service.findActiveStripeCustomerId("diego@example.com")).isEmpty();
    }

    @Test
    void findActivePremiumEmailsNormalizesAndDelegatesToRepository() {
        when(subscriberRepository.findActivePremiumEmails(
                Set.of("diego@example.com", "ana@example.com"),
                NOW))
                .thenReturn(Set.of("DIEGO@example.com"));

        assertThat(service.findActivePremiumEmails(List.of(
                " Diego@Example.com ",
                "ana@example.com",
                " ")))
                .containsExactly("diego@example.com");
    }

    @Test
    void findEmailByStripeCustomerIdReturnsNormalizedEmail() {
        Subscriber subscriber = subscriber(NOW.minusSeconds(120), null);
        subscriber.setEmail("Diego@Example.com");
        when(subscriberRepository.findByStripeCustomerId("cus_test_123")).thenReturn(Optional.of(subscriber));

        assertThat(service.findEmailByStripeCustomerId(" cus_test_123 "))
                .contains("diego@example.com");
    }

    @Test
    void findDueCancellationEmailsDelegatesToRepository() {
        Subscriber subscriber = subscriber(NOW.minusSeconds(120), NOW.minusSeconds(2_592_000));
        when(subscriberRepository.findDueCancellationEmails(NOW.minusSeconds(2_592_000)))
                .thenReturn(List.of(subscriber));

        assertThat(service.findDueCancellationEmails(NOW.minusSeconds(2_592_000)))
                .containsExactly(subscriber);
    }

    @Test
    void markCancellationEmailSentStoresTimestamp() {
        Subscriber subscriber = subscriber(NOW.minusSeconds(120), NOW.minusSeconds(2_592_000));
        when(subscriberRepository.findById(10L)).thenReturn(Optional.of(subscriber));

        assertThat(service.markCancellationEmailSent(10L, NOW)).isTrue();

        assertThat(subscriber.getCancellationEmailSentAt()).isEqualTo(NOW);
        verify(subscriberRepository).save(subscriber);
    }

    @Test
    void canAccessAdminDashboardAllowsActivePatronTier() {
        Subscriber subscriber = subscriber(NOW.minusSeconds(120), null);
        subscriber.setPlanTier("patron");
        when(subscriberRepository.findByEmail("diego@example.com")).thenReturn(Optional.of(subscriber));

        assertThat(service.canAccessAdminDashboard("diego@example.com")).isTrue();
    }

    @Test
    void canAccessAdminDashboardRejectsRegularPremiumTier() {
        Subscriber subscriber = subscriber(NOW.minusSeconds(120), null);
        subscriber.setPlanTier("PREMIUM");
        when(subscriberRepository.findByEmail("diego@example.com")).thenReturn(Optional.of(subscriber));

        assertThat(service.canAccessAdminDashboard("diego@example.com")).isFalse();
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
