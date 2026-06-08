package com.dondeanime.backend.premium;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class PremiumAccessTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-25T10:00:00Z");

    private final PremiumAccessTokenService service = new PremiumAccessTokenService(
            "test-secret",
            Clock.fixed(NOW, ZoneOffset.UTC),
            new ObjectMapper(),
            Duration.ofHours(1));

    @Test
    void resolvesSignedTokenEmail() {
        String token = service.createToken(new PremiumEntitlement(
                "diego@example.com",
                "PREMIUM",
                NOW.plusSeconds(3_600)));

        assertThat(service.resolveEmail(token)).contains("diego@example.com");
    }

    @Test
    void rejectsTamperedToken() {
        String token = service.createToken(new PremiumEntitlement(
                "diego@example.com",
                "PREMIUM",
                NOW.plusSeconds(3_600)));

        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(service.resolveEmail(tampered)).isEmpty();
    }

    @Test
    void rejectsExpiredToken() {
        PremiumAccessTokenService expiredService = new PremiumAccessTokenService(
                "test-secret",
                Clock.fixed(NOW, ZoneOffset.UTC),
                new ObjectMapper(),
                Duration.ofSeconds(1));
        String token = expiredService.createToken(new PremiumEntitlement(
                "diego@example.com",
                "PREMIUM",
                NOW.plusSeconds(1)));
        PremiumAccessTokenService laterService = new PremiumAccessTokenService(
                "test-secret",
                Clock.fixed(NOW.plusSeconds(2), ZoneOffset.UTC),
                new ObjectMapper(),
                Duration.ofHours(1));

        assertThat(laterService.resolveEmail(token)).isEmpty();
    }
}
