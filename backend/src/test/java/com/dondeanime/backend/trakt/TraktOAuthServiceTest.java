package com.dondeanime.backend.trakt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class TraktOAuthServiceTest {

    private final TraktClient traktClient = mock(TraktClient.class);
    private final TraktOAuthStateService stateService = new TraktOAuthStateService(
            Clock.fixed(Instant.parse("2026-05-27T10:00:00Z"), ZoneOffset.UTC),
            "client-secret");
    private final TraktOAuthService service = new TraktOAuthService(
            traktClient,
            stateService,
            "https://trakt.tv",
            "client-id",
            "client-secret",
            "https://api.dondeanime.com/api/trakt/oauth/callback");

    @Test
    void buildsAuthorizationUriWithSignedState() {
        URI uri = service.authorizationUri();

        assertThat(uri.toString()).startsWith("https://trakt.tv/oauth/authorize?");
        assertThat(uri.getQuery()).contains("response_type=code");
        assertThat(uri.getQuery()).contains("client_id=client-id");
        assertThat(uri.getQuery()).contains("redirect_uri=https://api.dondeanime.com/api/trakt/oauth/callback");
        assertThat(uri.getQuery()).contains("state=");
    }

    @Test
    void completesCallbackWithoutPersistingTokens() {
        String state = stateService.createState();
        when(traktClient.exchangeAuthorizationCode("oauth-code"))
                .thenReturn(new TraktOAuthTokenResponse(
                        "access-token",
                        "refresh-token",
                        "bearer",
                        7200L,
                        "public",
                        1780000000L));

        TraktOAuthCallbackResponse response = service.completeCallback(" oauth-code ", state, null);

        assertThat(response.connected()).isTrue();
        assertThat(response.provider()).isEqualTo("trakt");
        assertThat(response.accessTokenStored()).isFalse();
        assertThat(response.refreshTokenStored()).isFalse();
        assertThat(response.expiresInSeconds()).isEqualTo(7200);
        verify(traktClient).exchangeAuthorizationCode("oauth-code");
    }

    @Test
    void rejectsInvalidStateBeforeCallingTrakt() {
        assertThatThrownBy(() -> service.completeCallback("oauth-code", "bad-state", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
                .isEqualTo(400);

        verify(traktClient, never()).exchangeAuthorizationCode("oauth-code");
    }
}
