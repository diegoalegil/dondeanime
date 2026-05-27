package com.dondeanime.backend.trakt;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TraktOAuthController.class)
class TraktOAuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TraktOAuthService traktOAuthService;

    @Test
    void startRedirectsToTraktAuthorization() throws Exception {
        when(traktOAuthService.authorizationUri())
                .thenReturn(URI.create("https://trakt.tv/oauth/authorize?client_id=client-id"));

        mvc.perform(get("/api/trakt/oauth/start"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://trakt.tv/oauth/authorize?client_id=client-id"));
    }

    @Test
    void callbackReturnsSafeConnectionResponse() throws Exception {
        when(traktOAuthService.completeCallback("code", "state", null))
                .thenReturn(new TraktOAuthCallbackResponse(
                        true,
                        "trakt",
                        false,
                        false,
                        7200L,
                        "public",
                        "Cuenta Trakt conectada temporalmente."));

        mvc.perform(get("/api/trakt/oauth/callback")
                        .queryParam("code", "code")
                        .queryParam("state", "state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.provider").value("trakt"))
                .andExpect(jsonPath("$.accessTokenStored").value(false))
                .andExpect(jsonPath("$.refreshTokenStored").value(false));
    }
}
