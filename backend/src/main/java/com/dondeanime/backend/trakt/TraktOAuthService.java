package com.dondeanime.backend.trakt;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class TraktOAuthService {

    private final TraktClient traktClient;
    private final TraktOAuthStateService stateService;
    private final String oauthBase;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public TraktOAuthService(
            TraktClient traktClient,
            TraktOAuthStateService stateService,
            @Value("${trakt.oauth-base:https://trakt.tv}") String oauthBase,
            @Value("${trakt.client-id:}") String clientId,
            @Value("${trakt.client-secret:}") String clientSecret,
            @Value("${trakt.redirect-uri:}") String redirectUri) {
        this.traktClient = traktClient;
        this.stateService = stateService;
        this.oauthBase = oauthBase;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    public URI authorizationUri() {
        ensureConfigured();
        return UriComponentsBuilder.fromUriString(oauthBase)
                .path("/oauth/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", stateService.createState())
                .build()
                .toUri();
    }

    public TraktOAuthCallbackResponse completeCallback(String code, String state, String error) {
        ensureConfigured();
        if (error != null && !error.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "trakt_oauth_denied");
        }
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing_trakt_code");
        }
        if (!stateService.isValid(state)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_trakt_state");
        }

        TraktOAuthTokenResponse token = traktClient.exchangeAuthorizationCode(code.trim());
        if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "invalid_trakt_token_response");
        }

        return new TraktOAuthCallbackResponse(
                true,
                "trakt",
                false,
                false,
                token.expiresIn(),
                token.scope(),
                "Cuenta Trakt conectada temporalmente. Los tokens no se guardan hasta aprobar el almacenamiento cifrado.");
    }

    private void ensureConfigured() {
        if (isBlank(clientId) || isBlank(clientSecret) || isBlank(redirectUri)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "trakt_oauth_not_configured");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
