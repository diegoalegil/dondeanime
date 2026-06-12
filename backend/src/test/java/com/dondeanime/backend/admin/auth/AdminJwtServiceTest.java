package com.dondeanime.backend.admin.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class AdminJwtServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-25T12:00:00Z");

    @Test
    void createdAdminTokenIsValidUntilExpiration() {
        AdminJwtService service = new AdminJwtService("secret", Clock.fixed(NOW, ZoneOffset.UTC));

        String token = service.createAdminSession().token();

        assertThat(service.isValidAdminToken(token)).isTrue();
    }

    @Test
    void tokenExpiresAfterEightHours() {
        AdminJwtService issuer = new AdminJwtService("secret", Clock.fixed(NOW, ZoneOffset.UTC));
        String token = issuer.createAdminSession().token();
        AdminJwtService verifier = new AdminJwtService("secret", Clock.fixed(NOW.plusSeconds(8 * 60 * 60 + 1), ZoneOffset.UTC));

        assertThat(verifier.isValidAdminToken(token)).isFalse();
    }

    @Test
    void tamperedTokenIsRejected() {
        AdminJwtService service = new AdminJwtService("secret", Clock.fixed(NOW, ZoneOffset.UTC));
        String token = service.createAdminSession().token();
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(service.isValidAdminToken(tampered)).isFalse();
    }

    @Test
    void validClaimsExposeJtiAndExpiration() {
        AdminJwtService service = new AdminJwtService("secret", Clock.fixed(NOW, ZoneOffset.UTC));
        String token = service.createAdminSession().token();

        AdminTokenClaims claims = service.validClaims(token).orElseThrow();

        assertThat(claims.jti()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(claims.expiresAt()).isEqualTo(NOW.plusSeconds(8 * 60 * 60));
    }

    @Test
    void validClaimsRejectsTamperedToken() {
        AdminJwtService service = new AdminJwtService("secret", Clock.fixed(NOW, ZoneOffset.UTC));
        String token = service.createAdminSession().token();
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(service.validClaims(tampered)).isEmpty();
    }
}
