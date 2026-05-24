package com.dondeanime.backend.subscription;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;
import com.dondeanime.backend.email.EmailService;

class SubscriptionServiceTest {

    private final AppUserRepository userRepository = org.mockito.Mockito.mock(AppUserRepository.class);
    private final SubscriptionRepository subscriptionRepository = org.mockito.Mockito.mock(SubscriptionRepository.class);
    private final AnimeRepository animeRepository = org.mockito.Mockito.mock(AnimeRepository.class);
    private final EmailTokenService emailTokenService = org.mockito.Mockito.mock(EmailTokenService.class);
    private final EmailService emailService = org.mockito.Mockito.mock(EmailService.class);

    private final SubscriptionService service = new SubscriptionService(
            userRepository,
            subscriptionRepository,
            animeRepository,
            emailTokenService,
            emailService,
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
