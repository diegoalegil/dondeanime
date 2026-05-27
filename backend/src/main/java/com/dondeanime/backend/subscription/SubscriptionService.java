package com.dondeanime.backend.subscription;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;
import com.dondeanime.backend.email.EmailService;
import com.dondeanime.backend.premium.SubscriberService;

@Service
public class SubscriptionService {

    private static final int FREE_ALERT_LIMIT = 10;

    private final AppUserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AnimeRepository animeRepository;
    private final EmailTokenService emailTokenService;
    private final EmailService emailService;
    private final SubscriberService subscriberService;
    private final String apiUrl;

    public SubscriptionService(
            AppUserRepository userRepository,
            SubscriptionRepository subscriptionRepository,
            AnimeRepository animeRepository,
            EmailTokenService emailTokenService,
            EmailService emailService,
            SubscriberService subscriberService,
            @Value("${dondeanime.api-url}") String apiUrl) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.animeRepository = animeRepository;
        this.emailTokenService = emailTokenService;
        this.emailService = emailService;
        this.subscriberService = subscriberService;
        this.apiUrl = trimTrailingSlash(apiUrl);
    }

    @Transactional
    public void requestSubscription(SubscriptionRequest request) {
        String email = normalizeEmail(request.email());
        String countryCode = CountryCatalog.normalizeCountry(request.country());
        Anime anime = findAnime(request.animeSlug());
        Instant now = Instant.now();

        AppUser user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    AppUser created = new AppUser();
                    created.setEmail(email);
                    created.setCreatedAt(now);
                    return created;
                });

        if (user.getId() == null) {
            user = userRepository.save(user);
        }

        if (user.getConfirmedAt() == null) {
            IssuedEmailToken token = emailTokenService.createConfirmationToken(user, anime, countryCode);
            emailService.sendConfirmationEmail(
                    email,
                    animeTitle(anime),
                    CountryCatalog.countryName(countryCode),
                    confirmUrl(token.rawToken()));
            return;
        }

        user.setUnsubscribedAt(null);
        createSubscriptionIfAbsent(user, anime, countryCode, now);
    }

    @Transactional
    public ConfirmedSubscription confirmSubscription(String rawToken) {
        EmailToken token = emailTokenService.consumeConfirmationToken(rawToken);
        AppUser user = token.getUser();
        Anime anime = token.getAnime();
        Instant now = Instant.now();

        if (user.getConfirmedAt() == null) {
            user.setConfirmedAt(now);
        }
        user.setUnsubscribedAt(null);

        createSubscriptionIfAbsent(user, anime, token.getCountryCode(), now);

        return new ConfirmedSubscription(
                user.getEmail(),
                animeTitle(anime),
                CountryCatalog.countryName(token.getCountryCode()));
    }

    @Transactional
    public ConfirmedSubscription unsubscribe(String rawToken) {
        EmailToken token = emailTokenService.resolveUnsubscribeToken(rawToken);
        AppUser user = token.getUser();
        user.setUnsubscribedAt(Instant.now());

        return new ConfirmedSubscription(
                user.getEmail(),
                animeTitle(token.getAnime()),
                CountryCatalog.countryName(token.getCountryCode()));
    }

    @Transactional
    public void eraseUser(String email, String rawToken) {
        EmailToken token = emailTokenService.resolveUnsubscribeToken(rawToken);
        String normalizedEmail = normalizeEmail(email);
        AppUser user = token.getUser();

        if (!normalizedEmail.equals(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El token no pertenece a este email");
        }

        userRepository.delete(user);
    }

    public String eraseUrl(String email, String rawToken) {
        return apiUrl + "/api/users/" + encode(email) + "/erase?token=" + encode(rawToken);
    }

    public String unsubscribeUrl(String rawToken) {
        return apiUrl + "/api/subscriptions/unsubscribe?token=" + encode(rawToken);
    }

    private void createSubscriptionIfAbsent(AppUser user, Anime anime, String countryCode, Instant now) {
        subscriptionRepository.findByUser_IdAndAnime_IdAndCountryCode(user.getId(), anime.getId(), countryCode)
                .orElseGet(() -> {
                    assertAlertLimitAllows(user);
                    Subscription subscription = new Subscription();
                    subscription.setUser(user);
                    subscription.setAnime(anime);
                    subscription.setCountryCode(countryCode);
                    subscription.setCreatedAt(now);
                    return subscriptionRepository.save(subscription);
                });
    }

    private void assertAlertLimitAllows(AppUser user) {
        if (subscriberService.isPremium(user.getEmail())) {
            return;
        }

        long activeAlerts = subscriptionRepository.countByUser_IdAndNotifiedAtIsNull(user.getId());
        if (activeAlerts >= FREE_ALERT_LIMIT) {
            throw new ResponseStatusException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "El plan gratis permite hasta " + FREE_ALERT_LIMIT + " alertas activas");
        }
    }

    private Anime findAnime(String slug) {
        return animeRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anime no encontrado"));
    }

    private String confirmUrl(String rawToken) {
        return apiUrl + "/api/subscriptions/confirm?token=" + encode(rawToken);
    }

    private static String normalizeEmail(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email es obligatorio");
        }

        return rawEmail.trim().toLowerCase(Locale.ROOT);
    }

    private static String animeTitle(Anime anime) {
        return anime.getTitleEnglish() != null && !anime.getTitleEnglish().isBlank()
                ? anime.getTitleEnglish()
                : anime.getTitleRomaji();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
