package com.dondeanime.backend.trakt;

import com.fasterxml.jackson.annotation.JsonProperty;

record TraktOAuthTokenRequest(
        String code,
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
                clientId,
                clientSecret,
                redirectUri,
                "authorization_code");
    }
}
