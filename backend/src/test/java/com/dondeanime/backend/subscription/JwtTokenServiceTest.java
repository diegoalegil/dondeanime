package com.dondeanime.backend.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

    private final JwtTokenService service = new JwtTokenService("test-secret-with-enough-entropy");

    @Test
    void createTokenProducesValidSignature() {
        String token = service.createToken(EmailToken.TYPE_CONFIRMATION, Duration.ofMinutes(5));

        assertThat(token.split("\\.")).hasSize(3);
        assertThat(service.hasValidSignature(token)).isTrue();
    }

    @Test
    void invalidTokensDoNotPassSignatureValidation() {
        String token = service.createToken(EmailToken.TYPE_CONFIRMATION, Duration.ofMinutes(5));
        String tampered = token.substring(0, token.length() - 1) + "x";

        assertThat(service.hasValidSignature(null)).isFalse();
        assertThat(service.hasValidSignature("not-a-jwt")).isFalse();
        assertThat(service.hasValidSignature(tampered)).isFalse();
    }

    @Test
    void hashIsDeterministicSha256Hex() {
        String first = service.hash("raw.jwt");
        String second = service.hash("raw.jwt");

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64);
    }
}
