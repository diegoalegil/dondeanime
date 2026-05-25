package com.dondeanime.backend.api;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class LegacyApiRedirectController {

    public static final String SUNSET_DATE = "Wed, 25 Nov 2026 00:00:00 GMT";

    @GetMapping({
            "/api/anime",
            "/api/anime/upcoming",
            "/api/anime/{slug}",
            "/api/providers",
            "/api/providers/{slug}/{country}",
            "/api/genres",
            "/api/genres/{slug}",
            "/api/seasons",
            "/api/seasons/{year}/{season}",
            "/api/sitemap"
    })
    public ResponseEntity<Void> redirectToVersionedApi(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String versionedPath = "/api/v1" + requestUri.substring("/api".length());
        URI location = ServletUriComponentsBuilder.fromRequest(request)
                .replacePath(versionedPath)
                .build(true)
                .toUri();

        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .location(location)
                .header("Deprecation", "true")
                .header("Sunset", SUNSET_DATE)
                .header("Link", "</docs/api-versioning.md>; rel=\"deprecation\"; type=\"text/markdown\"")
                .build();
    }
}
