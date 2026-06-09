package com.dondeanime.backend.config;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final List<RateLimitRule> RULES = List.of(
            new RateLimitRule("/api/search", false, 30),
            new RateLimitRule("/api/chat/search", false, 20),
            new RateLimitRule("/api/track/affiliate", false, 60),
            new RateLimitRule("/api/track/recommendation", false, 60),
            new RateLimitRule("/api/trakt/sync", false, 10),
            new RateLimitRule("/api/trakt/watched", false, 60),
            new RateLimitRule("/api/trakt/oauth/", true, 20),
            // Endpoints que disparan envío de email o crean sesiones de pago:
            // límite bajo por IP para frenar email-bombing y abuso de Stripe.
            new RateLimitRule("/api/newsletter/subscribe", false, 5),
            new RateLimitRule("/api/subscriptions/unsubscribe", false, 10),
            new RateLimitRule("/api/subscriptions", false, 5),
            new RateLimitRule("/api/alerts", false, 5),
            new RateLimitRule("/api/push/subscribe", false, 10),
            new RateLimitRule("/api/mobile/push/register", false, 10),
            new RateLimitRule("/api/premium/checkout", false, 5),
            new RateLimitRule("/api/premium/portal", false, 5),
            new RateLimitRule("/api/users/", true, 5),
            new RateLimitRule("/api/admin/", true, 10));

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        // El preflight CORS no debe consumir cuota de rate limit.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        RateLimitRule rule = findRule(request.getRequestURI());
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Bucket bucket = buckets.computeIfAbsent(bucketKey(rule, request), ignored -> createBucket(rule.requestsPerMinute()));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"rate_limit_exceeded\"}");
    }

    private static Bucket createBucket(int requestsPerMinute) {
        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(requestsPerMinute)
                        .refillGreedy(requestsPerMinute, Duration.ofMinutes(1)))
                .build();
    }

    private static RateLimitRule findRule(String path) {
        return RULES.stream()
                .filter(rule -> rule.matches(path))
                .findFirst()
                .orElse(null);
    }

    private static String bucketKey(RateLimitRule rule, HttpServletRequest request) {
        return rule.path() + "|" + clientIp(request);
    }

    /**
     * IP real del cliente detrás de Caddy. Solo se confía en el ÚLTIMO salto
     * de X-Forwarded-For (lo escribe Caddy, no el cliente). No se mira
     * CF-Connecting-IP: no hay Cloudflare delante y cualquier cliente podría
     * mandar un valor aleatorio por petición para esquivar el rate limit.
     */
    private static String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] hops = forwardedFor.split(",");
            String realIp = hops[hops.length - 1].trim();
            if (!realIp.isBlank()) {
                return realIp;
            }
        }
        return request.getRemoteAddr();
    }

    private record RateLimitRule(String path, boolean prefix, int requestsPerMinute) {
        boolean matches(String requestPath) {
            return prefix ? requestPath.startsWith(path) : requestPath.equals(path);
        }
    }
}
