package com.dondeanime.backend.trakt;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trakt/oauth")
public class TraktOAuthController {

    private final TraktOAuthService traktOAuthService;

    public TraktOAuthController(TraktOAuthService traktOAuthService) {
        this.traktOAuthService = traktOAuthService;
    }

    @GetMapping("/start")
    public ResponseEntity<Void> start() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(traktOAuthService.authorizationUri())
                .build();
    }

    @GetMapping("/callback")
    public TraktOAuthCallbackResponse callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {
        return traktOAuthService.completeCallback(code, state, error);
    }
}
