package com.dondeanime.backend.trakt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TraktOAuthStateService {

    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private static final Duration CLOCK_SKEW = Duration.ofMinutes(1);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final Clock clock;
    private final String signingSecret;

    public TraktOAuthStateService(
            Clock clock,
            @Value("${trakt.client-secret:}") String signingSecret) {
        this.clock = clock;
        this.signingSecret = signingSecret;
    }

    public String createState() {
        ensureSecret();
        String payload = Instant.now(clock).getEpochSecond() + ":" + UUID.randomUUID();
        String token = payload + ":" + sign(payload);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isValid(String state) {
        if (state == null || state.isBlank() || signingSecret == null || signingSecret.isBlank()) {
            return false;
        }

        String decoded;
        try {
            decoded = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return false;
        }

        String[] parts = decoded.split(":", 3);
        if (parts.length != 3) {
            return false;
        }

        String payload = parts[0] + ":" + parts[1];
        if (!constantTimeEquals(sign(payload), parts[2])) {
            return false;
        }

        try {
            Instant issuedAt = Instant.ofEpochSecond(Long.parseLong(parts[0]));
            Instant now = Instant.now(clock);
            return !issuedAt.isBefore(now.minus(STATE_TTL))
                    && !issuedAt.isAfter(now.plus(CLOCK_SKEW));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String sign(String payload) {
        ensureSecret();
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el estado OAuth de Trakt", e);
        }
    }

    private void ensureSecret() {
        if (signingSecret == null || signingSecret.isBlank()) {
            throw new IllegalStateException("TRAKT_CLIENT_SECRET no configurado");
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
