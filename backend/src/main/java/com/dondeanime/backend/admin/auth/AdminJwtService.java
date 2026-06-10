package com.dondeanime.backend.admin.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AdminJwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Duration TOKEN_TTL = Duration.ofHours(8);
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final Pattern TOKEN_TYPE_PATTERN = Pattern.compile("\"typ\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern EXPIRATION_PATTERN = Pattern.compile("\"exp\"\\s*:\\s*(\\d+)");

    private final byte[] secret;
    private final Clock clock;

    @Autowired
    public AdminJwtService(@Value("${admin.jwt-secret:${alerts.jwt-secret}}") String secret) {
        this(secret, Clock.systemUTC());
    }

    AdminJwtService(String secret, Clock clock) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    public AdminLoginResponse createAdminSession() {
        Instant expiresAt = clock.instant().plus(TOKEN_TTL);
        String token = createToken(expiresAt);
        return new AdminLoginResponse(token, "Bearer", expiresAt);
    }

    public boolean isValidAdminToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }

        String[] parts = rawToken.split("\\.");
        if (parts.length != 3) {
            return false;
        }

        String unsigned = parts[0] + "." + parts[1];
        if (!MessageDigest.isEqual(
                sign(unsigned).getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            return false;
        }

        try {
            String payload = new String(BASE64_URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8);
            return hasAdminType(payload) && hasValidExpiration(payload);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String createToken(Instant expiresAt) {
        String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = encode("""
                {"typ":"admin","jti":"%s","exp":%d}
                """.formatted(UUID.randomUUID(), expiresAt.getEpochSecond()).trim());
        String unsigned = header + "." + payload;
        return unsigned + "." + sign(unsigned);
    }

    private boolean hasAdminType(String payload) {
        Matcher matcher = TOKEN_TYPE_PATTERN.matcher(payload);
        return matcher.find() && "admin".equals(matcher.group(1));
    }

    private boolean hasValidExpiration(String payload) {
        Matcher matcher = EXPIRATION_PATTERN.matcher(payload);
        if (!matcher.find()) {
            return false;
        }

        Instant expiresAt = Instant.ofEpochSecond(Long.parseLong(matcher.group(1)));
        return expiresAt.isAfter(clock.instant());
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar token admin", e);
        }
    }

    private static String encode(String value) {
        return BASE64_URL_ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
