package com.dondeanime.backend.trakt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class TraktAccessTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-10T10:00:00Z");

    private final TraktAccessTokenService service = new TraktAccessTokenService(
            "test-secret",
            Clock.fixed(NOW, ZoneOffset.UTC),
            new ObjectMapper(),
            Duration.ofHours(1));

    @Test
    void resolvesSignedTokenExternalUserId() {
        String token = service.createToken("user-123");

        assertThat(service.resolveExternalUserId(token)).contains("user-123");
    }

    @Test
    void resolvesFromAuthorizationHeader() {
        String token = service.createToken("user-123");

        assertThat(service.resolveFromAuthorizationHeader("Bearer " + token)).contains("user-123");
        assertThat(service.resolveFromAuthorizationHeader(token)).isEmpty();
        assertThat(service.resolveFromAuthorizationHeader(null)).isEmpty();
    }

    @Test
    void rejectsTamperedToken() {
        String token = service.createToken("user-123");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(service.resolveExternalUserId(tampered)).isEmpty();
    }

    @Test
    void rejectsTokenSignedWithOtherSecret() {
        TraktAccessTokenService otherSecret = new TraktAccessTokenService(
                "other-secret",
                Clock.fixed(NOW, ZoneOffset.UTC),
                new ObjectMapper(),
                Duration.ofHours(1));
        String token = otherSecret.createToken("user-123");

        assertThat(service.resolveExternalUserId(token)).isEmpty();
    }

    @Test
    void rejectsExpiredToken() {
        TraktAccessTokenService expiredService = new TraktAccessTokenService(
                "test-secret",
                Clock.fixed(NOW, ZoneOffset.UTC),
                new ObjectMapper(),
                Duration.ofSeconds(1));
        String token = expiredService.createToken("user-123");
        TraktAccessTokenService laterService = new TraktAccessTokenService(
                "test-secret",
                Clock.fixed(NOW.plusSeconds(2), ZoneOffset.UTC),
                new ObjectMapper(),
                Duration.ofHours(1));

        assertThat(laterService.resolveExternalUserId(token)).isEmpty();
    }
}
