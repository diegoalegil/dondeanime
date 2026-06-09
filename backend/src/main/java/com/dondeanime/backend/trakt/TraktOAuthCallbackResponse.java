package com.dondeanime.backend.trakt;

public record TraktOAuthCallbackResponse(
        boolean connected,
        String provider,
        String externalUserId,
        boolean accessTokenStored,
        boolean refreshTokenStored,
        Long expiresInSeconds,
        String scope,
        String message) {
}
