package com.dondeanime.backend.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;
import com.dondeanime.backend.email.EmailService;
import com.dondeanime.backend.premium.SubscriberService;

class SubscriptionServiceTest {

    private final AppUserRepository userRepository = org.mockito.Mockito.mock(AppUserRepository.class);
    private final SubscriptionRepository subscriptionRepository = org.mockito.Mockito.mock(SubscriptionRepository.class);
    private final AnimeRepository animeRepository = org.mockito.Mockito.mock(AnimeRepository.class);
    private final EmailTokenService emailTokenService = org.mockito.Mockito.mock(EmailTokenService.class);
    private final EmailService emailService = org.mockito.Mockito.mock(EmailService.class);
    private final SubscriberService subscriberService = org.mockito.Mockito.mock(SubscriberService.class);

    private final SubscriptionService service = new SubscriptionService(
            userRepository,
            subscriptionRepository,
            animeRepository,
            emailTokenService,
            emailService,
            subscriberService,
            "https://api.dondeanime.com");

    @Test
    void unconfirmedUserReceivesConfirmationEmail() {
        Anime anime = anime();
        AppUser savedUser = user(null);
        savedUser.setId(10L);

        when(animeRepository.findBySlug("attack-on-titan")).thenReturn(Optional.of(anime));
        when(userRepository.findByEmail("diego@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(AppUser.class))).thenReturn(savedUser);
        when(emailTokenService.createConfirmationToken(savedUser, anime, "ES"))
                .thenReturn(new IssuedEmailToken("raw.jwt", token(savedUser, anime, "ES")));

        service.requestSubscription(new SubscriptionRequest(
                "Diego@Example.com",
                "attack-on-titan",
                "es"));

        verify(emailService).sendConfirmationEmail(
                eq("diego@example.com"),
                eq("Attack on Titan"),
                eq("España"),
                eq("https://api.dondeanime.com/api/subscriptions/confirm?token=raw.jwt"));
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    void confirmedUserWithExistingSubscriptionDoesNotDuplicateIt() {
        Anime anime = anime();
        AppUser user = user(Instant.now());
        user.setId(10L);
        Subscription existing = new Subscription();
        existing.setUser(user);
        existing.setAnime(anime);
        existing.setCountryCode("ES");

        when(animeRepository.findBySlug("attack-on-titan")).thenReturn(Optional.of(anime));
        when(userRepository.findByEmail("diego@example.com")).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUser_IdAndAnime_IdAndCountryCode(10L, 1L, "ES"))
                .thenReturn(Optional.of(existing));

        service.requestSubscription(new SubscriptionRequest(
                "diego@example.com",
                "attack-on-titan",
                "ES"));

        verify(subscriptionRepository, never()).save(any(Subscription.class));
        verify(emailService, never()).sendConfirmationEmail(any(), any(), any(), any());
        verifyNoInteractions(subscriberService);
    }

    @Test
    void freeUserCannotCreateMoreThanTenActiveAlerts() {
        Anime anime = anime();
        AppUser user = user(Instant.now());
        user.setId(10L);

        when(animeRepository.findBySlug("attack-on-titan")).thenReturn(Optional.of(anime));
        when(userRepository.findByEmail("diego@example.com")).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUser_IdAndAnime_IdAndCountryCode(10L, 1L, "ES"))
                .thenReturn(Optional.empty());
        when(subscriberService.isPremium("diego@example.com")).thenReturn(false);
        when(subscriptionRepository.countByUser_IdAndNotifiedAtIsNull(10L)).thenReturn(10L);

        assertThatThrownBy(() -> service.requestSubscription(new SubscriptionRequest(
                "diego@example.com",
                "attack-on-titan",
                "ES")))
                .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED));

        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    void premiumUserBypassesFreeAlertLimit() {
        Anime anime = anime();
        AppUser user = user(Instant.now());
        user.setId(10L);

        when(animeRepository.findBySlug("attack-on-titan")).thenReturn(Optional.of(anime));
        when(userRepository.findByEmail("diego@example.com")).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUser_IdAndAnime_IdAndCountryCode(10L, 1L, "ES"))
                .thenReturn(Optional.empty());
        when(subscriberService.isPremium("diego@example.com")).thenReturn(true);
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.requestSubscription(new SubscriptionRequest(
                "diego@example.com",
                "attack-on-titan",
                "ES"));

        verify(subscriptionRepository).save(any(Subscription.class));
        verify(subscriptionRepository, never()).countByUser_IdAndNotifiedAtIsNull(10L);
    }

    private static Anime anime() {
        Anime anime = new Anime();
        anime.setId(1L);
        anime.setSlug("attack-on-titan");
        anime.setTitleEnglish("Attack on Titan");
        anime.setTitleRomaji("Shingeki no Kyojin");
        return anime;
    }

    private static AppUser user(Instant confirmedAt) {
        AppUser user = new AppUser();
        user.setEmail("diego@example.com");
        user.setCreatedAt(Instant.now());
        user.setConfirmedAt(confirmedAt);
        return user;
    }

    private static EmailToken token(AppUser user, Anime anime, String countryCode) {
        EmailToken token = new EmailToken();
        token.setUser(user);
        token.setAnime(anime);
        token.setCountryCode(countryCode);
        token.setTokenType(EmailToken.TYPE_CONFIRMATION);
        token.setTokenHash("hash");
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plusSeconds(900));
        return token;
    }
}
