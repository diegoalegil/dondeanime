package com.dondeanime.backend.newsletter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.dondeanime.backend.subscription.JwtTokenService;

class NewsletterTokenServiceTest {

    private final NewsletterTokenRepository repository = org.mockito.Mockito.mock(NewsletterTokenRepository.class);
    private final JwtTokenService jwtTokenService = new JwtTokenService("test-secret");
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-25T12:00:00Z"), ZoneOffset.UTC);
    private final NewsletterTokenService service = new NewsletterTokenService(repository, jwtTokenService, clock);

    @Test
    void createConfirmationTokenStoresHashAndExpiration() {
        when(repository.save(any(NewsletterToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IssuedNewsletterToken issued = service.createConfirmationToken(subscriber());

        assertThat(issued.rawToken()).isNotBlank();
        assertThat(issued.entity().getTokenHash()).hasSize(64);
        assertThat(issued.entity().getExpiresAt()).isEqualTo(Instant.parse("2026-05-25T12:15:00Z"));
    }

    @Test
    void consumeConfirmationTokenRejectsUsedToken() {
        String rawToken = jwtTokenService.createToken(NewsletterToken.TYPE_CONFIRMATION, java.time.Duration.ofMinutes(15));
        NewsletterToken token = token(subscriber());
        token.setTokenHash(jwtTokenService.hash(rawToken));
        token.setUsedAt(Instant.parse("2026-05-25T12:01:00Z"));
        when(repository.findByTokenHash(jwtTokenService.hash(rawToken))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.consumeConfirmationToken(rawToken))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Token ya usado");
    }

    private static NewsletterSubscriber subscriber() {
        NewsletterSubscriber subscriber = new NewsletterSubscriber();
        subscriber.setId(10L);
        subscriber.setEmail("diego@example.com");
        return subscriber;
    }

    private static NewsletterToken token(NewsletterSubscriber subscriber) {
        NewsletterToken token = new NewsletterToken();
        token.setSubscriber(subscriber);
        token.setTokenType(NewsletterToken.TYPE_CONFIRMATION);
        token.setCreatedAt(Instant.parse("2026-05-25T12:00:00Z"));
        token.setExpiresAt(Instant.parse("2026-05-25T12:15:00Z"));
        return token;
    }
}
