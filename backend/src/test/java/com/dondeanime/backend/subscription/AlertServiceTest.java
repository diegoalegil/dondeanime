package com.dondeanime.backend.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.email.EmailService;
import com.dondeanime.backend.provider.WatchProvider;

class AlertServiceTest {

    private final SubscriptionRepository subscriptionRepository = org.mockito.Mockito.mock(SubscriptionRepository.class);
    private final EmailTokenService emailTokenService = org.mockito.Mockito.mock(EmailTokenService.class);
    private final SubscriptionService subscriptionService = org.mockito.Mockito.mock(SubscriptionService.class);
    private final EmailService emailService = org.mockito.Mockito.mock(EmailService.class);

    private final AlertService alertService = new AlertService(
            subscriptionRepository,
            emailTokenService,
            subscriptionService,
            emailService);

    @Test
    void sendsAlertAndMarksSubscriptionAsNotified() {
        Anime anime = anime();
        AppUser user = user();
        Subscription subscription = subscription(user, anime);
        WatchProvider provider = provider("Crunchyroll");
        EmailToken token = token(user, anime);

        when(subscriptionRepository.findPendingAlerts(1L, "ES")).thenReturn(List.of(subscription));
        when(emailTokenService.createUnsubscribeToken(user, anime, "ES"))
                .thenReturn(new IssuedEmailToken("raw.jwt", token));
        when(subscriptionService.unsubscribeUrl("raw.jwt")).thenReturn("https://api/unsubscribe");
        when(subscriptionService.eraseUrl("diego@example.com", "raw.jwt")).thenReturn("https://api/erase");

        int sent = alertService.notifyNewProviders(anime, Map.of("ES", List.of(provider)));

        assertThat(sent).isEqualTo(1);
        assertThat(subscription.getNotifiedAt()).isNotNull();
        verify(emailService).sendAlertEmail(
                "diego@example.com",
                "Attack on Titan",
                "España",
                List.of("Crunchyroll"),
                "https://api/unsubscribe",
                "https://api/erase");
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void skipsEmailWhenThereAreNoPendingSubscriptions() {
        Anime anime = anime();
        WatchProvider provider = provider("Crunchyroll");

        when(subscriptionRepository.findPendingAlerts(1L, "ES")).thenReturn(List.of());

        int sent = alertService.notifyNewProviders(anime, Map.of("ES", List.of(provider)));

        assertThat(sent).isZero();
        verify(emailService, never()).sendAlertEmail(any(), any(), any(), any(), any(), any());
    }

    private static Anime anime() {
        Anime anime = new Anime();
        anime.setId(1L);
        anime.setSlug("attack-on-titan");
        anime.setTitleEnglish("Attack on Titan");
        return anime;
    }

    private static AppUser user() {
        AppUser user = new AppUser();
        user.setId(10L);
        user.setEmail("diego@example.com");
        user.setCreatedAt(Instant.now());
        user.setConfirmedAt(Instant.now());
        return user;
    }

    private static Subscription subscription(AppUser user, Anime anime) {
        Subscription subscription = new Subscription();
        subscription.setId(20L);
        subscription.setUser(user);
        subscription.setAnime(anime);
        subscription.setCountryCode("ES");
        subscription.setCreatedAt(Instant.now());
        return subscription;
    }

    private static WatchProvider provider(String providerName) {
        WatchProvider provider = new WatchProvider();
        provider.setAnimeId(1L);
        provider.setCountryCode("ES");
        provider.setProviderName(providerName);
        provider.setProviderType("FLATRATE");
        provider.setUpdatedAt(Instant.now());
        return provider;
    }

    private static EmailToken token(AppUser user, Anime anime) {
        EmailToken token = new EmailToken();
        token.setUser(user);
        token.setAnime(anime);
        token.setCountryCode("ES");
        token.setTokenType(EmailToken.TYPE_UNSUBSCRIBE);
        token.setTokenHash("hash");
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        return token;
    }
}
