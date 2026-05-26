package com.dondeanime.backend.api;

import java.io.IOException;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LegacyApiRedirectController extends OncePerRequestFilter {

    public static final String SUNSET_DATE = "Wed, 25 Nov 2026 00:00:00 GMT";

    private static final Set<String> ANIME_ACTION_PATHS = Set.of(
            "sync",
            "match",
            "sync-providers");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (isDeprecatedPublicRoute(request.getRequestURI())) {
            response.setHeader("Deprecation", "true");
            response.setHeader("Sunset", SUNSET_DATE);
            response.setHeader(
                    "Link",
                    "</docs/api-versioning.md>; rel=\"deprecation\"; type=\"text/markdown\"");
        }

        filterChain.doFilter(request, response);
    }

    private static boolean isDeprecatedPublicRoute(String path) {
        if (path.equals("/api/anime")
                || path.equals("/api/anime/upcoming")
                || path.equals("/api/providers")
                || path.equals("/api/genres")
                || path.equals("/api/seasons")
                || path.equals("/api/sitemap")) {
            return true;
        }

        if (path.startsWith("/api/anime/")) {
            String[] segments = path.substring("/api/anime/".length()).split("/");
            return (segments.length == 1 && !ANIME_ACTION_PATHS.contains(segments[0]))
                    || segments.length == 2 && "similar".equals(segments[1]);
        }

        return hasSegments(path, "/api/providers/", 2)
                || hasSegments(path, "/api/genres/", 1)
                || hasSegments(path, "/api/seasons/", 2);
    }

    private static boolean hasSegments(String path, String prefix, int expectedSegments) {
        if (!path.startsWith(prefix)) {
            return false;
        }
        String rest = path.substring(prefix.length());
        if (rest.isBlank()) {
            return false;
        }
        return rest.split("/").length == expectedSegments;
    }
}
