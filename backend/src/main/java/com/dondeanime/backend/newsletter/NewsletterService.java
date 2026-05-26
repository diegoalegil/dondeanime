package com.dondeanime.backend.newsletter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.dondeanime.backend.email.EmailService;

@Service
public class NewsletterService {

    private final NewsletterSubscriberRepository subscriberRepository;
    private final NewsletterTokenService tokenService;
    private final EmailService emailService;
    private final String apiUrl;
    private final Clock clock;

    public NewsletterService(
            NewsletterSubscriberRepository subscriberRepository,
            NewsletterTokenService tokenService,
            EmailService emailService,
            @Value("${dondeanime.api-url}") String apiUrl,
            Clock clock) {
        this.subscriberRepository = subscriberRepository;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.apiUrl = trimTrailingSlash(apiUrl);
        this.clock = clock;
    }

    @Transactional
    public void requestSubscription(NewsletterSubscribeRequest request) {
        String email = normalizeEmail(request.email());

        NewsletterSubscriber subscriber = subscriberRepository.findByEmail(email)
                .orElseGet(() -> {
                    NewsletterSubscriber created = new NewsletterSubscriber();
                    created.setEmail(email);
                    return created;
                });

        if (subscriber.getId() == null) {
            subscriber = subscriberRepository.save(subscriber);
        }

        if (subscriber.getConfirmedAt() != null && subscriber.getUnsubscribedAt() == null) {
            return;
        }

        IssuedNewsletterToken token = tokenService.createConfirmationToken(subscriber);
        emailService.sendNewsletterConfirmationEmail(email, confirmUrl(token.rawToken()));
    }

    @Transactional
    public String confirmSubscription(String rawToken) {
        NewsletterToken token = tokenService.consumeConfirmationToken(rawToken);
        NewsletterSubscriber subscriber = token.getSubscriber();
        if (subscriber.getConfirmedAt() == null) {
            subscriber.setConfirmedAt(Instant.now(clock));
        }
        subscriber.setUnsubscribedAt(null);
        return subscriber.getEmail();
    }

    private String confirmUrl(String rawToken) {
        return apiUrl + "/api/newsletter/confirm?token=" + encode(rawToken);
    }

    private static String normalizeEmail(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email es obligatorio");
        }
        return rawEmail.trim().toLowerCase(Locale.ROOT);
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
