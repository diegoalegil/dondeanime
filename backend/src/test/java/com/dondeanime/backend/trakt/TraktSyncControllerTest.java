package com.dondeanime.backend.trakt;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TraktSyncController.class)
class TraktSyncControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TraktSyncService traktSyncService;

    @MockitoBean
    private TraktAccessTokenService accessTokenService;

    @Test
    void syncReturnsImportSummaryWithoutTokens() throws Exception {
        when(accessTokenService.resolveFromAuthorizationHeader("Bearer valid-token"))
                .thenReturn(Optional.of("user-123"));
        when(traktSyncService.sync("user-123"))
                .thenReturn(new TraktSyncResponse(
                        1,
                        1,
                        1,
                        List.of(new TraktUnmatchedItem(
                                "WATCHED",
                                "Unknown Show",
                                2020,
                                "No existe match local por titulo y anio"))));

        mvc.perform(post("/api/trakt/sync")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.watchedImported").value(1))
                .andExpect(jsonPath("$.ratingsImported").value(1))
                .andExpect(jsonPath("$.unmatchedCount").value(1))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test
    void rejectsSyncWithoutToken() throws Exception {
        when(accessTokenService.resolveFromAuthorizationHeader(null))
                .thenReturn(Optional.empty());

        mvc.perform(post("/api/trakt/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalUserId":"user-123"}
                                """))
                .andExpect(status().isUnauthorized());

        verify(traktSyncService, never()).sync(anyString());
    }

    @Test
    void rejectsSyncWithInvalidToken() throws Exception {
        when(accessTokenService.resolveFromAuthorizationHeader("Bearer forged"))
                .thenReturn(Optional.empty());

        mvc.perform(post("/api/trakt/sync")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer forged"))
                .andExpect(status().isUnauthorized());

        verify(traktSyncService, never()).sync(anyString());
    }
}
