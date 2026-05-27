package com.dondeanime.backend.trakt;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TraktOAuthTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") Long expiresIn,
        String scope,
        @JsonProperty("created_at") Long createdAt) {
}
