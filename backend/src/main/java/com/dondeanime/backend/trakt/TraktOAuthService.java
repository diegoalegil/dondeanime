package com.dondeanime.backend.trakt;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class TraktOAuthService {

    private static final String PROVIDER = "trakt";

    private final TraktClient traktClient;
    private final TraktOAuthStateService stateService;
    private final ExternalAccountService externalAccountService;
    private final TraktTokenCipherService tokenCipherService;
    private final Clock clock;
    private final boolean enabled;
    private final String oauthBase;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public TraktOAuthService(
            TraktClient traktClient,
            TraktOAuthStateService stateService,
            ExternalAccountService externalAccountService,
            TraktTokenCipherService tokenCipherService,
            Clock clock,
            @Value("${trakt.enabled:false}") boolean enabled,
            @Value("${trakt.oauth-base:https://trakt.tv}") String oauthBase,
            @Value("${trakt.client-id:}") String clientId,
            @Value("${trakt.client-secret:}") String clientSecret,
            @Value("${trakt.redirect-uri:}") String redirectUri) {
        this.traktClient = traktClient;
        this.stateService = stateService;
        this.externalAccountService = externalAccountService;
        this.tokenCipherService = tokenCipherService;
        this.clock = clock;
        this.enabled = enabled;
        this.oauthBase = oauthBase;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    public URI authorizationUri() {
        ensureEnabled();
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
        ensureEnabled();
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

        TraktOAuthTokenResponse token = exchangeAuthorizationCode(code.trim());
        if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "invalid_trakt_token_response");
        }
        if (token.refreshToken() == null || token.refreshToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "invalid_trakt_token_response");
        }

        TraktUserSettingsResponse userSettings = fetchCurrentUser(token.accessToken());
        String externalUserId = userSettings == null ? null : userSettings.externalUserId();
        if (externalUserId == null || externalUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "invalid_trakt_user_response");
        }

        externalAccountService.upsert(new ExternalAccountUpsertCommand(
                PROVIDER,
                externalUserId,
                null,
                tokenCipherService.encrypt(token.accessToken()),
                tokenCipherService.encrypt(token.refreshToken()),
                token.scope(),
                expiresAt(token)));

        return new TraktOAuthCallbackResponse(
                true,
                PROVIDER,
                externalUserId,
                true,
                true,
                token.expiresIn(),
                token.scope(),
                "Cuenta Trakt conectada. Los tokens se guardan cifrados.");
    }

    private TraktOAuthTokenResponse exchangeAuthorizationCode(String code) {
        try {
            return traktClient.exchangeAuthorizationCode(code);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "trakt_oauth_failed", e);
        }
    }

    private TraktUserSettingsResponse fetchCurrentUser(String accessToken) {
        try {
            return traktClient.fetchCurrentUser(accessToken);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "trakt_oauth_failed", e);
        }
    }

    private Instant expiresAt(TraktOAuthTokenResponse token) {
        if (token.expiresIn() == null) {
            return null;
        }
        Instant issuedAt = token.createdAt() == null
                ? Instant.now(clock)
                : Instant.ofEpochSecond(token.createdAt());
        return issuedAt.plusSeconds(token.expiresIn());
    }

    private void ensureEnabled() {
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "trakt_disabled");
        }
    }

    private void ensureConfigured() {
        if (isBlank(clientId)
                || isBlank(clientSecret)
                || isBlank(redirectUri)
                || !tokenCipherService.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "trakt_oauth_not_configured");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
