package com.dondeanime.backend.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.provider.WatchProvider;
import com.dondeanime.backend.subscription.AppUser;
import com.dondeanime.backend.subscription.Subscription;
import com.dondeanime.backend.subscription.SubscriptionRepository;

class PushNotificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-25T10:00:00Z");

    private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
    private final PushSubscriptionRepository pushSubscriptionRepository = mock(PushSubscriptionRepository.class);
    private final PushSubscriptionCleanupService cleanupService = new PushSubscriptionCleanupService(
            pushSubscriptionRepository,
            Clock.fixed(NOW, ZoneOffset.UTC));
    private final WebPushService webPushService = mock(WebPushService.class);
    private final PushNotificationService service = new PushNotificationService(
            subscriptionRepository,
            pushSubscriptionRepository,
            cleanupService,
            webPushService);

    @Test
    void sendsPushOnlyToPendingAlertEmailsInCountry() {
        Anime anime = anime();
        PushSubscription push = pushSubscription("diego@example.com");
        when(subscriptionRepository.findPendingAlerts(1L, "ES"))
                .thenReturn(List.of(subscription("DIEGO@example.com"), subscription("ana@example.com")));
        when(pushSubscriptionRepository.findByCountryIsoAndUserEmailInOrderByCreatedAtAsc(
                eq("ES"),
                anyCollection()))
                .thenReturn(List.of(push));
        when(webPushService.send(eq(push), anyString())).thenReturn(Optional.of(201));

        int sent = service.notifyNewProviders(anime, "es", List.of(
                provider("Netflix"),
                provider("Crunchyroll"),
                provider("Netflix")));

        assertThat(sent).isEqualTo(1);
        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<Collection<String>> emailsCaptor = ArgumentCaptor.forClass((Class) Collection.class);
        verify(pushSubscriptionRepository).findByCountryIsoAndUserEmailInOrderByCreatedAtAsc(
                eq("ES"),
                emailsCaptor.capture());
        assertThat(emailsCaptor.getValue()).containsExactly("diego@example.com", "ana@example.com");

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(webPushService).send(eq(push), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue())
                .contains("\"title\":\"Attack on Titan ya esta disponible\"")
                .contains("\"body\":\"Nuevo en ES: Crunchyroll, Netflix.\"")
                .contains("\"url\":\"/anime/attack-on-titan\"")
                .contains("\"tag\":\"provider-added-1-ES\"");
    }

    @Test
    void skipsPushWhenThereAreNoPendingEmailAlerts() {
        when(subscriptionRepository.findPendingAlerts(1L, "ES")).thenReturn(List.of());

        int sent = service.notifyNewProviders(anime(), "ES", List.of(provider("Crunchyroll")));

        assertThat(sent).isZero();
        verifyNoInteractions(pushSubscriptionRepository, webPushService);
    }

    @Test
    void countsOnlyAcceptedPushResponses() {
        PushSubscription accepted = pushSubscription("diego@example.com");
        PushSubscription rejected = pushSubscription("ana@example.com");
        when(subscriptionRepository.findPendingAlerts(1L, "MX"))
                .thenReturn(List.of(subscription("diego@example.com"), subscription("ana@example.com")));
        when(pushSubscriptionRepository.findByCountryIsoAndUserEmailInOrderByCreatedAtAsc(
                eq("MX"),
                anyCollection()))
                .thenReturn(List.of(accepted, rejected));
        when(webPushService.send(eq(accepted), anyString())).thenReturn(Optional.of(201));
        when(webPushService.send(eq(rejected), anyString())).thenReturn(Optional.of(500));

        int sent = service.notifyNewProviders(anime(), "mx", List.of(provider("Crunchyroll")));

        assertThat(sent).isEqualTo(1);
    }

    @Test
    void deletesBouncingSubscriptionsWhenPushReturnsGone() {
        PushSubscription bounced = pushSubscription("diego@example.com");
        when(subscriptionRepository.findPendingAlerts(1L, "ES"))
                .thenReturn(List.of(subscription("diego@example.com")));
        when(pushSubscriptionRepository.findByCountryIsoAndUserEmailInOrderByCreatedAtAsc(
                eq("ES"),
                anyCollection()))
                .thenReturn(List.of(bounced));
        when(webPushService.send(eq(bounced), anyString())).thenReturn(Optional.of(410));

        int sent = service.notifyNewProviders(anime(), "ES", List.of(provider("Crunchyroll")));

        assertThat(sent).isZero();
        verify(pushSubscriptionRepository).delete(bounced);
    }

    private static Anime anime() {
        Anime anime = new Anime();
        anime.setId(1L);
        anime.setSlug("attack-on-titan");
        anime.setTitleEnglish("Attack on Titan");
        anime.setTitleRomaji("Shingeki no Kyojin");
        return anime;
    }

    private static WatchProvider provider(String name) {
        WatchProvider provider = new WatchProvider();
        provider.setProviderName(name);
        return provider;
    }

    private static Subscription subscription(String email) {
        AppUser user = new AppUser();
        user.setEmail(email);
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        return subscription;
    }

    private static PushSubscription pushSubscription(String email) {
        PushSubscription pushSubscription = new PushSubscription();
        pushSubscription.setUserEmail(email);
        pushSubscription.setEndpoint("https://push.example/" + email);
        pushSubscription.setP256dh("p256dh");
        pushSubscription.setAuth("auth");
        pushSubscription.setCountryIso("ES");
        return pushSubscription;
    }
}
