package com.dondeanime.backend.admin.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class AdminTotpServiceTest {

    private static final String RFC_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    @Test
    void validatesKnownTotpCode() {
        AdminTotpService service = new AdminTotpService(Clock.fixed(Instant.ofEpochSecond(59), ZoneOffset.UTC));

        assertThat(service.isValidCode(RFC_SECRET, "287082")).isTrue();
    }

    @Test
    void rejectsWrongCode() {
        AdminTotpService service = new AdminTotpService(Clock.fixed(Instant.ofEpochSecond(59), ZoneOffset.UTC));

        assertThat(service.isValidCode(RFC_SECRET, "000000")).isFalse();
    }

    @Test
    void acceptsAdjacentTimeStep() {
        AdminTotpService issuer = new AdminTotpService(Clock.fixed(Instant.ofEpochSecond(59), ZoneOffset.UTC));
        String code = issuer.generateCode(RFC_SECRET, 1);
        AdminTotpService verifier = new AdminTotpService(Clock.fixed(Instant.ofEpochSecond(89), ZoneOffset.UTC));

        assertThat(verifier.isValidCode(RFC_SECRET, code)).isTrue();
    }

    @Test
    void setupUriContainsIssuerAndSecret() {
        AdminTotpService service = new AdminTotpService(Clock.systemUTC());

        String secret = service.generateSecret();
        String uri = service.buildOtpAuthUri("admin", secret);

        assertThat(secret).matches("[A-Z2-7]+");
        assertThat(uri).contains("otpauth://totp/DondeAnime%3Aadmin");
        assertThat(uri).contains("secret=" + secret);
        assertThat(uri).contains("issuer=DondeAnime");
    }
}
