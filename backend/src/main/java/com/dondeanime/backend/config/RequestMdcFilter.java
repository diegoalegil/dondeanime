package com.dondeanime.backend.config;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RequestMdcFilter extends OncePerRequestFilter {

    static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final Set<String> ANIME_ACTION_PATHS = Set.of(
            "sync",
            "match",
            "sync-providers");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(REQUEST_ID_HEADER);
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString();
        }

        try {
            MDC.put("trace_id", traceId);
            extractAnimeSlug(request.getRequestURI()).ifPresent(slug -> MDC.put("anime_slug", slug));
            response.setHeader(REQUEST_ID_HEADER, traceId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("anime_slug");
            MDC.remove("trace_id");
        }
    }

    private static Optional<String> extractAnimeSlug(String requestUri) {
        String prefix = "/api/anime/";
        if (!requestUri.startsWith(prefix)) {
            return Optional.empty();
        }

        String rest = requestUri.substring(prefix.length());
        int slashIndex = rest.indexOf('/');
        String firstSegment = slashIndex >= 0 ? rest.substring(0, slashIndex) : rest;
        if (!StringUtils.hasText(firstSegment) || ANIME_ACTION_PATHS.contains(firstSegment)) {
            return Optional.empty();
        }
        return Optional.of(firstSegment);
    }
}
