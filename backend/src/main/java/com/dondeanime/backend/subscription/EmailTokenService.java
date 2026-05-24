package com.dondeanime.backend.subscription;

import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.dondeanime.backend.anime.Anime;

@Service
public class EmailTokenService {

    private static final Duration CONFIRMATION_TTL = Duration.ofMinutes(15);
    private static final Duration UNSUBSCRIBE_TTL = Duration.ofDays(365);

    private final EmailTokenRepository repository;
    private final JwtTokenService jwtTokenService;

    public EmailTokenService(
            EmailTokenRepository repository,
            JwtTokenService jwtTokenService) {
        this.repository = repository;
        this.jwtTokenService = jwtTokenService;
    }

    public IssuedEmailToken createConfirmationToken(AppUser user, Anime anime, String countryCode) {
        return createToken(EmailToken.TYPE_CONFIRMATION, user, anime, countryCode, CONFIRMATION_TTL);
    }

    public IssuedEmailToken createUnsubscribeToken(AppUser user, Anime anime, String countryCode) {
        return createToken(EmailToken.TYPE_UNSUBSCRIBE, user, anime, countryCode, UNSUBSCRIBE_TTL);
    }

    public EmailToken consumeConfirmationToken(String rawToken) {
        EmailToken token = resolveToken(rawToken, EmailToken.TYPE_CONFIRMATION, true);
        token.setUsedAt(Instant.now());
        return repository.save(token);
    }

    public EmailToken resolveUnsubscribeToken(String rawToken) {
        return resolveToken(rawToken, EmailToken.TYPE_UNSUBSCRIBE, false);
    }

    private IssuedEmailToken createToken(
            String tokenType,
            AppUser user,
            Anime anime,
            String countryCode,
            Duration ttl) {
        String rawToken = jwtTokenService.createToken(tokenType, ttl);
        Instant now = Instant.now();

        EmailToken token = new EmailToken();
        token.setUser(user);
        token.setAnime(anime);
        token.setCountryCode(countryCode);
        token.setTokenType(tokenType);
        token.setTokenHash(jwtTokenService.hash(rawToken));
        token.setCreatedAt(now);
        token.setExpiresAt(now.plus(ttl));

        return new IssuedEmailToken(rawToken, repository.save(token));
    }

    private EmailToken resolveToken(String rawToken, String expectedType, boolean requireUnused) {
        if (!jwtTokenService.hasValidSignature(rawToken)) {
            throw invalidToken();
        }

        EmailToken token = repository.findByTokenHash(jwtTokenService.hash(rawToken))
                .orElseThrow(EmailTokenService::invalidToken);

        if (!expectedType.equals(token.getTokenType())) {
            throw invalidToken();
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Token caducado");
        }

        if (requireUnused && token.getUsedAt() != null) {
            throw new ResponseStatusException(HttpStatus.GONE, "Token ya usado");
        }

        return token;
    }

    private static ResponseStatusException invalidToken() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido");
    }
}
