package com.dondeanime.backend.subscription;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.provider.ProviderAddedEvent;
import com.dondeanime.backend.provider.WatchProvider;
import com.dondeanime.backend.push.PushNotificationService;

@SpringJUnitConfig(ProviderAddedEventListenerIntegrationTest.Config.class)
class ProviderAddedEventListenerIntegrationTest {

    @jakarta.annotation.Resource
    private ApplicationEventPublisher eventPublisher;

    @jakarta.annotation.Resource
    private AlertService alertService;

    @jakarta.annotation.Resource
    private PushNotificationService pushNotificationService;

    @Test
    void publishedProviderAddedEventIsProcessedByAlertService() {
        Anime anime = anime();
        WatchProvider provider = provider();
        when(pushNotificationService.notifyNewProviders(anime, "ES", List.of(provider)))
                .thenReturn(1);
        when(alertService.notifyNewProviders(eq(anime), argThat(map ->
                map.containsKey("ES") && map.get("ES").equals(List.of(provider)))))
                .thenReturn(1);

        eventPublisher.publishEvent(new ProviderAddedEvent(anime, "ES", List.of(provider)));

        verify(pushNotificationService).notifyNewProviders(anime, "ES", List.of(provider));
        verify(alertService).notifyNewProviders(eq(anime), argThat(map ->
                map.containsKey("ES") && map.get("ES").equals(List.of(provider))));
    }

    private static Anime anime() {
        Anime anime = new Anime();
        anime.setId(1L);
        anime.setSlug("attack-on-titan");
        anime.setTitleEnglish("Attack on Titan");
        return anime;
    }

    private static WatchProvider provider() {
        WatchProvider provider = new WatchProvider();
        provider.setAnimeId(1L);
        provider.setCountryCode("ES");
        provider.setProviderName("Crunchyroll");
        provider.setProviderType("FLATRATE");
        provider.setUpdatedAt(Instant.now());
        return provider;
    }

    @Configuration
    static class Config {

        @Bean
        AlertService alertService() {
            return mock(AlertService.class);
        }

        @Bean
        PushNotificationService pushNotificationService() {
            return mock(PushNotificationService.class);
        }

        @Bean
        ProviderAddedEventListener providerAddedEventListener(
                AlertService alertService,
                PushNotificationService pushNotificationService) {
            return new ProviderAddedEventListener(alertService, pushNotificationService);
        }
    }
}
