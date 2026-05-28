package com.dondeanime.backend.trakt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TraktClientTest {

    @Test
    void exchangesAuthorizationCodeWithTrakt() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TraktClient client = new TraktClient(
                builder,
                "https://api.trakt.tv",
                "client-id",
                "client-secret",
                "https://api.dondeanime.com/api/trakt/oauth/callback");

        server.expect(once(), requestTo("https://api.trakt.tv/oauth/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.code").value("oauth-code"))
                .andExpect(jsonPath("$.client_id").value("client-id"))
                .andExpect(jsonPath("$.client_secret").value("client-secret"))
                .andExpect(jsonPath("$.redirect_uri").value("https://api.dondeanime.com/api/trakt/oauth/callback"))
                .andExpect(jsonPath("$.grant_type").value("authorization_code"))
                .andRespond(withSuccess("""
                        {
                          "access_token": "access-token",
                          "refresh_token": "refresh-token",
                          "token_type": "bearer",
                          "expires_in": 7200,
                          "scope": "public",
                          "created_at": 1780000000
                        }
                        """, MediaType.APPLICATION_JSON));

        TraktOAuthTokenResponse response = client.exchangeAuthorizationCode("oauth-code");

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.expiresIn()).isEqualTo(7200);
        server.verify();
    }
}
