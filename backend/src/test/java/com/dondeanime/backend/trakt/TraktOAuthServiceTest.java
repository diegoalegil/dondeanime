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
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

class TraktOAuthServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-27T10:00:00Z"),
            ZoneOffset.UTC);

    private final TraktClient traktClient = mock(TraktClient.class);
    private final TraktOAuthStateService stateService = new TraktOAuthStateService(
            CLOCK,
            "client-secret");
    private final ExternalAccountService externalAccountService = mock(ExternalAccountService.class);
    private final TraktTokenCipherService tokenCipherService =
            new TraktTokenCipherService("test-secret-with-enough-entropy");
    private final TraktAccessTokenService accessTokenService = new TraktAccessTokenService(
            "test-secret",
            CLOCK,
            new com.fasterxml.jackson.databind.ObjectMapper(),
            java.time.Duration.ofDays(30));
    private final TraktOAuthService service = new TraktOAuthService(
            traktClient,
            stateService,
            externalAccountService,
            tokenCipherService,
            accessTokenService,
            CLOCK,
            true,
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
    void completesCallbackAndStoresEncryptedTokens() {
        String state = stateService.createState();
        when(traktClient.exchangeAuthorizationCode("oauth-code"))
                .thenReturn(new TraktOAuthTokenResponse(
                        "access-token",
                        "refresh-token",
                        "bearer",
                        7200L,
                        "public",
                        1780000000L));
        when(traktClient.fetchCurrentUser("access-token"))
                .thenReturn(new TraktUserSettingsResponse(
                        new TraktUser("Diego", new TraktUserIds("diego"))));

        TraktOAuthCallbackResponse response = service.completeCallback(" oauth-code ", state, null);

        assertThat(response.connected()).isTrue();
        assertThat(response.provider()).isEqualTo("trakt");
        assertThat(response.externalUserId()).isEqualTo("diego");
        assertThat(accessTokenService.resolveExternalUserId(response.apiAccessToken())).contains("diego");
        assertThat(response.accessTokenStored()).isTrue();
        assertThat(response.refreshTokenStored()).isTrue();
        assertThat(response.expiresInSeconds()).isEqualTo(7200);
        verify(traktClient).exchangeAuthorizationCode("oauth-code");
        verify(traktClient).fetchCurrentUser("access-token");

        ArgumentCaptor<ExternalAccountUpsertCommand> commandCaptor =
                ArgumentCaptor.forClass(ExternalAccountUpsertCommand.class);
        verify(externalAccountService).upsert(commandCaptor.capture());
        ExternalAccountUpsertCommand command = commandCaptor.getValue();
        assertThat(command.provider()).isEqualTo("trakt");
        assertThat(command.externalUserId()).isEqualTo("diego");
        assertThat(command.accessTokenCiphertext()).doesNotContain("access-token");
        assertThat(command.refreshTokenCiphertext()).doesNotContain("refresh-token");
        assertThat(tokenCipherService.decrypt(command.accessTokenCiphertext())).isEqualTo("access-token");
        assertThat(tokenCipherService.decrypt(command.refreshTokenCiphertext())).isEqualTo("refresh-token");
        assertThat(command.tokenExpiresAt()).isEqualTo("2026-05-28T22:26:40Z");
    }

    @Test
    void rejectsInvalidStateBeforeCallingTrakt() {
        assertThatThrownBy(() -> service.completeCallback("oauth-code", "bad-state", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
                .isEqualTo(400);

        verify(traktClient, never()).exchangeAuthorizationCode("oauth-code");
    }

    @Test
    void disabledFeatureRejectsBeforeCallingTrakt() {
        TraktOAuthService disabled = new TraktOAuthService(
                traktClient,
                stateService,
                externalAccountService,
                tokenCipherService,
                accessTokenService,
                CLOCK,
                false,
                "https://trakt.tv",
                "client-id",
                "client-secret",
                "https://api.dondeanime.com/api/trakt/oauth/callback");

        assertThatThrownBy(disabled::authorizationUri)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
                .isEqualTo(503);

        verify(traktClient, never()).exchangeAuthorizationCode("oauth-code");
    }
}
