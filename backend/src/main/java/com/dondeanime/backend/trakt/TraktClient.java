package com.dondeanime.backend.trakt;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TraktClient {

    private static final String TRAKT_API_VERSION = "2";

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public TraktClient(
            RestClient.Builder restClientBuilder,
            @Value("${trakt.api-base:https://api.trakt.tv}") String apiBase,
            @Value("${trakt.client-id:}") String clientId,
            @Value("${trakt.client-secret:}") String clientSecret,
            @Value("${trakt.redirect-uri:}") String redirectUri) {
        this.restClient = restClientBuilder.baseUrl(apiBase).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    public TraktOAuthTokenResponse exchangeAuthorizationCode(String code) {
        return restClient.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(TraktOAuthTokenRequest.authorizationCode(
                        code,
                        clientId,
                        clientSecret,
                        redirectUri))
                .retrieve()
                .body(TraktOAuthTokenResponse.class);
    }

    public TraktOAuthTokenResponse refreshAccessToken(String refreshToken) {
        return restClient.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(TraktOAuthTokenRequest.refreshToken(
                        refreshToken,
                        clientId,
                        clientSecret,
                        redirectUri))
                .retrieve()
                .body(TraktOAuthTokenResponse.class);
    }

    public TraktUserSettingsResponse fetchCurrentUser(String accessToken) {
        return restClient.get()
                .uri("/users/settings")
                .headers(headers -> applyHeaders(headers, accessToken))
                .retrieve()
                .body(TraktUserSettingsResponse.class);
    }

    private void applyHeaders(HttpHeaders headers, String accessToken) {
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("trakt-api-version", TRAKT_API_VERSION);
        headers.set("trakt-api-key", clientId);
    }
}
