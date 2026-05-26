package com.dondeanime.backend.newsletter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.dondeanime.backend.email.EmailService;

class NewsletterServiceTest {

    private final NewsletterSubscriberRepository subscriberRepository =
            org.mockito.Mockito.mock(NewsletterSubscriberRepository.class);
    private final NewsletterTokenService tokenService = org.mockito.Mockito.mock(NewsletterTokenService.class);
    private final EmailService emailService = org.mockito.Mockito.mock(EmailService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-25T12:00:00Z"), ZoneOffset.UTC);

    private final NewsletterService service = new NewsletterService(
            subscriberRepository,
            tokenService,
            emailService,
            "https://api.dondeanime.com",
            clock);

    @Test
    void newSubscriberReceivesConfirmationEmail() {
        NewsletterSubscriber saved = subscriber("diego@example.com");
        saved.setId(10L);
        NewsletterToken token = token(saved);

        when(subscriberRepository.findByEmail("diego@example.com")).thenReturn(Optional.empty());
        when(subscriberRepository.save(any(NewsletterSubscriber.class))).thenReturn(saved);
        when(tokenService.createConfirmationToken(saved)).thenReturn(new IssuedNewsletterToken("raw.jwt", token));

        service.requestSubscription(new NewsletterSubscribeRequest(" Diego@Example.com ", true));

        verify(emailService).sendNewsletterConfirmationEmail(
                "diego@example.com",
                "https://api.dondeanime.com/api/newsletter/confirm?token=raw.jwt");
    }

    @Test
    void activeConfirmedSubscriberDoesNotReceiveDuplicateEmail() {
        NewsletterSubscriber subscriber = subscriber("diego@example.com");
        subscriber.setId(10L);
        subscriber.setConfirmedAt(Instant.parse("2026-05-25T12:00:00Z"));
        when(subscriberRepository.findByEmail("diego@example.com")).thenReturn(Optional.of(subscriber));

        service.requestSubscription(new NewsletterSubscribeRequest("diego@example.com", true));

        verify(tokenService, never()).createConfirmationToken(any());
        verify(emailService, never()).sendNewsletterConfirmationEmail(any(), any());
    }

    @Test
    void confirmMarksSubscriberAsConfirmedAndResubscribed() {
        NewsletterSubscriber subscriber = subscriber("diego@example.com");
        subscriber.setId(10L);
        subscriber.setUnsubscribedAt(Instant.parse("2026-05-24T12:00:00Z"));
        NewsletterToken token = token(subscriber);
        when(tokenService.consumeConfirmationToken("raw.jwt")).thenReturn(token);

        service.confirmSubscription("raw.jwt");

        org.assertj.core.api.Assertions.assertThat(subscriber.getConfirmedAt())
                .isEqualTo(Instant.parse("2026-05-25T12:00:00Z"));
        org.assertj.core.api.Assertions.assertThat(subscriber.getUnsubscribedAt()).isNull();
    }

    private static NewsletterSubscriber subscriber(String email) {
        NewsletterSubscriber subscriber = new NewsletterSubscriber();
        subscriber.setEmail(email);
        return subscriber;
    }

    private static NewsletterToken token(NewsletterSubscriber subscriber) {
        NewsletterToken token = new NewsletterToken();
        token.setSubscriber(subscriber);
        token.setTokenType(NewsletterToken.TYPE_CONFIRMATION);
        token.setTokenHash("hash");
        token.setCreatedAt(Instant.parse("2026-05-25T12:00:00Z"));
        token.setExpiresAt(Instant.parse("2026-05-25T12:15:00Z"));
        return token;
    }
}
