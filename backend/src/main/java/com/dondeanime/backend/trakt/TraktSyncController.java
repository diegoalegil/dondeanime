package com.dondeanime.backend.trakt;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class TraktSyncController {

    private final TraktSyncService traktSyncService;
    private final TraktAccessTokenService accessTokenService;

    public TraktSyncController(
            TraktSyncService traktSyncService,
            TraktAccessTokenService accessTokenService) {
        this.traktSyncService = traktSyncService;
        this.accessTokenService = accessTokenService;
    }

    @PostMapping("/api/trakt/sync")
    public TraktSyncResponse sync(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String externalUserId = accessTokenService.resolveFromAuthorizationHeader(authorization)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_trakt_token"));
        return traktSyncService.sync(externalUserId);
    }
}
