package com.dondeanime.backend.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WebPushServiceTest {

    private final PushSubscriptionRepository repository = mock(PushSubscriptionRepository.class);
    private final WebPushService service = new WebPushService(repository, "", "", "mailto:contacto@dondeanime.com");

    @Test
    void savesNewSubscriptionWithNormalizedCountryAndEmail() {
        PushSubscriptionRequest request = request("DIEGO@EXAMPLE.COM", "es");
        when(repository.findByEndpoint("https://push.example/123")).thenReturn(Optional.empty());
        when(repository.save(org.mockito.Mockito.any(PushSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PushSubscription saved = service.saveSubscription(request);

        assertThat(saved.getUserEmail()).isEqualTo("diego@example.com");
        assertThat(saved.getCountryIso()).isEqualTo("ES");
        assertThat(saved.getCreatedAt()).isNotNull();
        verify(repository).save(saved);
    }

    @Test
    void updatesExistingSubscriptionWithoutReplacingCreatedAt() {
        Instant createdAt = Instant.parse("2026-05-25T00:00:00Z");
        PushSubscription existing = new PushSubscription();
        existing.setId(10L);
        existing.setCreatedAt(createdAt);
        existing.setEndpoint("https://push.example/123");
        when(repository.findByEndpoint("https://push.example/123")).thenReturn(Optional.of(existing));
        when(repository.save(org.mockito.Mockito.any(PushSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.saveSubscription(request("diego@example.com", "MX"));

        ArgumentCaptor<PushSubscription> captor = ArgumentCaptor.forClass(PushSubscription.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(10L);
        assertThat(captor.getValue().getCreatedAt()).isEqualTo(createdAt);
        assertThat(captor.getValue().getCountryIso()).isEqualTo("MX");
    }

    @Test
    void reportsUnconfiguredWhenVapidKeysAreBlank() {
        assertThat(service.isConfigured()).isFalse();
    }

    private static PushSubscriptionRequest request(String email, String countryIso) {
        return new PushSubscriptionRequest(
                email,
                "https://push.example/123",
                new PushSubscriptionKeys("p256dh", "auth"),
                countryIso);
    }
}
