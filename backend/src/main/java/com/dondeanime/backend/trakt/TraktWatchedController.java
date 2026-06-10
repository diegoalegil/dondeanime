package com.dondeanime.backend.trakt;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class TraktWatchedController {

    private final TraktWatchedService traktWatchedService;
    private final TraktAccessTokenService accessTokenService;

    public TraktWatchedController(
            TraktWatchedService traktWatchedService,
            TraktAccessTokenService accessTokenService) {
        this.traktWatchedService = traktWatchedService;
        this.accessTokenService = accessTokenService;
    }

    @GetMapping("/api/trakt/watched")
    public TraktWatchedResponse watched(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String externalUserId = accessTokenService.resolveFromAuthorizationHeader(authorization)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_trakt_token"));
        return traktWatchedService.watched(externalUserId);
    }
}
