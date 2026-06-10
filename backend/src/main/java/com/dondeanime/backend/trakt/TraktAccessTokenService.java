package com.dondeanime.backend.trakt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TraktAccessTokenService {

    public static final String BEARER_PREFIX = "Bearer ";

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String TOKEN_TYPE = "trakt";
    private static final Duration DEFAULT_TTL = Duration.ofDays(30);
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final byte[] secret;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    @Autowired
    public TraktAccessTokenService(
            @Value("${trakt.access-token-secret:${alerts.jwt-secret}}") String secret,
            Clock clock) {
        this(secret, clock, new ObjectMapper(), DEFAULT_TTL);
    }

    TraktAccessTokenService(String secret, Clock clock, ObjectMapper objectMapper, Duration ttl) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    public String createToken(String externalUserId) {
        Map<String, Object> header = Map.of(
                "alg", "HS256",
                "typ", "JWT");
        Map<String, Object> payload = Map.of(
                "typ", TOKEN_TYPE,
                "sub", externalUserId,
                "exp", clock.instant().plus(ttl).getEpochSecond());
        String unsigned = encodeJson(header) + "." + encodeJson(payload);
        return unsigned + "." + sign(unsigned);
    }

    public Optional<String> resolveExternalUserId(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        String[] parts = rawToken.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }
        String unsigned = parts[0] + "." + parts[1];
        if (!MessageDigest.isEqual(
                sign(unsigned).getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            return Optional.empty();
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(
                    BASE64_URL_DECODER.decode(parts[1]),
                    MAP_TYPE);
            if (!TOKEN_TYPE.equals(payload.get("typ"))) {
                return Optional.empty();
            }
            Object expiration = payload.get("exp");
            if (!(expiration instanceof Number number)
                    || !Instant.ofEpochSecond(number.longValue()).isAfter(clock.instant())) {
                return Optional.empty();
            }
            Object subject = payload.get("sub");
            if (!(subject instanceof String value) || value.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(value.trim());
        } catch (IllegalArgumentException | IOException e) {
            return Optional.empty();
        }
    }

    public Optional<String> resolveFromAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        return resolveExternalUserId(authorizationHeader.substring(BEARER_PREFIX.length()).trim());
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo crear el token Trakt", e);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el token Trakt", e);
        }
    }
}
