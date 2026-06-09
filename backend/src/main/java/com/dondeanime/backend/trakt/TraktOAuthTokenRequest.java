package com.dondeanime.backend.trakt;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
record TraktOAuthTokenRequest(
        String code,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("client_id") String clientId,
        @JsonProperty("client_secret") String clientSecret,
        @JsonProperty("redirect_uri") String redirectUri,
        @JsonProperty("grant_type") String grantType) {

    static TraktOAuthTokenRequest authorizationCode(
            String code,
            String clientId,
            String clientSecret,
            String redirectUri) {
        return new TraktOAuthTokenRequest(
                code,
                null,
                clientId,
                clientSecret,
                redirectUri,
                "authorization_code");
    }

    static TraktOAuthTokenRequest refreshToken(
            String refreshToken,
            String clientId,
            String clientSecret,
            String redirectUri) {
        return new TraktOAuthTokenRequest(
                null,
                refreshToken,
                clientId,
                clientSecret,
                redirectUri,
                "refresh_token");
    }
}
