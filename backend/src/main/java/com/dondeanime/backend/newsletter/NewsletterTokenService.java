package com.dondeanime.backend.newsletter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.dondeanime.backend.subscription.JwtTokenService;

@Service
public class NewsletterTokenService {

    private static final Duration CONFIRMATION_TTL = Duration.ofMinutes(15);

    private final NewsletterTokenRepository repository;
    private final JwtTokenService jwtTokenService;
    private final Clock clock;

    public NewsletterTokenService(
            NewsletterTokenRepository repository,
            JwtTokenService jwtTokenService,
            Clock clock) {
        this.repository = repository;
        this.jwtTokenService = jwtTokenService;
        this.clock = clock;
    }

    public IssuedNewsletterToken createConfirmationToken(NewsletterSubscriber subscriber) {
        String rawToken = jwtTokenService.createToken(NewsletterToken.TYPE_CONFIRMATION, CONFIRMATION_TTL);
        Instant now = Instant.now(clock);

        NewsletterToken token = new NewsletterToken();
        token.setSubscriber(subscriber);
        token.setTokenType(NewsletterToken.TYPE_CONFIRMATION);
        token.setTokenHash(jwtTokenService.hash(rawToken));
        token.setCreatedAt(now);
        token.setExpiresAt(now.plus(CONFIRMATION_TTL));

        return new IssuedNewsletterToken(rawToken, repository.save(token));
    }

    public NewsletterToken consumeConfirmationToken(String rawToken) {
        NewsletterToken token = resolveToken(rawToken, true);
        token.setUsedAt(Instant.now(clock));
        return repository.save(token);
    }

    private NewsletterToken resolveToken(String rawToken, boolean requireUnused) {
        if (!jwtTokenService.hasValidSignature(rawToken)) {
            throw invalidToken();
        }

        NewsletterToken token = repository.findByTokenHash(jwtTokenService.hash(rawToken))
                .orElseThrow(NewsletterTokenService::invalidToken);

        if (!NewsletterToken.TYPE_CONFIRMATION.equals(token.getTokenType())) {
            throw invalidToken();
        }

        if (token.getExpiresAt().isBefore(Instant.now(clock))) {
            throw new ResponseStatusException(HttpStatus.GONE, "Token caducado");
        }

        if (requireUnused && token.getUsedAt() != null) {
            throw new ResponseStatusException(HttpStatus.GONE, "Token ya usado");
        }

        return token;
    }

    private static ResponseStatusException invalidToken() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalido");
    }
}
