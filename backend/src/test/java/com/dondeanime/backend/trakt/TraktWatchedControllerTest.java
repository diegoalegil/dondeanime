package com.dondeanime.backend.trakt;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TraktWatchedController.class)
class TraktWatchedControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TraktWatchedService traktWatchedService;

    @MockitoBean
    private TraktAccessTokenService accessTokenService;

    @Test
    void returnsWatchedSlugsForTokenOwner() throws Exception {
        when(accessTokenService.resolveFromAuthorizationHeader("Bearer valid-token"))
                .thenReturn(Optional.of("user-123"));
        when(traktWatchedService.watched("user-123"))
                .thenReturn(new TraktWatchedResponse(List.of("attack-on-titan")));

        mvc.perform(get("/api/trakt/watched")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slugs[0]").value("attack-on-titan"))
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void rejectsRequestWithoutToken() throws Exception {
        when(accessTokenService.resolveFromAuthorizationHeader(null))
                .thenReturn(Optional.empty());

        mvc.perform(get("/api/trakt/watched").queryParam("externalUserId", "user-123"))
                .andExpect(status().isUnauthorized());

        verify(traktWatchedService, never()).watched(anyString());
    }

    @Test
    void rejectsRequestWithInvalidToken() throws Exception {
        when(accessTokenService.resolveFromAuthorizationHeader("Bearer forged"))
                .thenReturn(Optional.empty());

        mvc.perform(get("/api/trakt/watched")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer forged"))
                .andExpect(status().isUnauthorized());

        verify(traktWatchedService, never()).watched(anyString());
    }
}
