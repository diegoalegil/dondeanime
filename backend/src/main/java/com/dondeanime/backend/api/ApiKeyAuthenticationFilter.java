package com.dondeanime.backend.api;

import java.io.IOException;
import java.time.Duration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
    public static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";

    private static final Duration MONTHLY_REFILL = Duration.ofDays(31);

    private final ApiKeyService apiKeyService;
    // Caffeine con tope de tamaño y TTL largo (el bucket es MENSUAL: el TTL debe
    // superar el ciclo de refill). Acota la memoria sin reiniciar el límite
    // mensual de las claves activas; una clave inactiva 35 días se evicciona.
    private final Cache<String, BucketState> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofDays(35))
            .maximumSize(10_000)
            .build();

    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!requiresApiKey(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawKey = request.getHeader(API_KEY_HEADER);
        if (rawKey == null || rawKey.isBlank()) {
            writeError(response, HttpStatus.UNAUTHORIZED, "missing_api_key");
            return;
        }

        ApiKeyUsage usage;
        try {
            usage = apiKeyService.findUsage(rawKey);
        } catch (ApiKeyNotFoundException e) {
            writeError(response, HttpStatus.UNAUTHORIZED, "invalid_api_key");
            return;
        }

        ConsumptionProbe probe = bucketFor(rawKey.trim(), usage.monthlyQuota()).tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            writeRateLimitHeaders(response, usage.monthlyQuota(), 0);
            writeError(response, HttpStatus.TOO_MANY_REQUESTS, "rate_limit_exceeded");
            return;
        }

        try {
            usage = apiKeyService.recordUsage(rawKey, request.getRequestURI());
        } catch (ApiKeyNotFoundException e) {
            writeError(response, HttpStatus.UNAUTHORIZED, "invalid_api_key");
            return;
        } catch (ApiQuotaExceededException e) {
            writeRateLimitHeaders(response, usage.monthlyQuota(), 0);
            writeError(response, HttpStatus.TOO_MANY_REQUESTS, "monthly_quota_exceeded");
            return;
        }

        writeRateLimitHeaders(response, usage.monthlyQuota(), Math.min(probe.getRemainingTokens(), usage.remaining()));
        filterChain.doFilter(request, response);
    }

    private boolean requiresApiKey(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/")
                && !path.startsWith("/api/v1/docs");
    }

    private Bucket bucketFor(String key, long monthlyQuota) {
        BucketState state = buckets.asMap().compute(key, (ignored, existing) -> {
            if (existing != null && existing.monthlyQuota() == monthlyQuota) {
                return existing;
            }
            Bandwidth bandwidth = Bandwidth.builder()
                    .capacity(monthlyQuota)
                    .refillIntervally(monthlyQuota, MONTHLY_REFILL)
                    .build();
            return new BucketState(monthlyQuota, Bucket.builder().addLimit(bandwidth).build());
        });
        return state.bucket();
    }

    private static void writeRateLimitHeaders(HttpServletResponse response, long limit, long remaining) {
        response.setHeader(RATE_LIMIT_LIMIT_HEADER, Long.toString(limit));
        response.setHeader(RATE_LIMIT_REMAINING_HEADER, Long.toString(Math.max(0, remaining)));
    }

    private static void writeError(HttpServletResponse response, HttpStatus status, String error) throws IOException {
        response.setStatus(status.value());
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + error + "\"}");
    }

    private record BucketState(long monthlyQuota, Bucket bucket) {}
}
