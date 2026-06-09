package com.dondeanime.backend.trakt;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TraktSyncController.class)
class TraktSyncControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TraktSyncService traktSyncService;

    @Test
    void syncReturnsImportSummaryWithoutTokens() throws Exception {
        when(traktSyncService.sync(new TraktSyncRequest("user-123")))
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalUserId":"user-123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.watchedImported").value(1))
                .andExpect(jsonPath("$.ratingsImported").value(1))
                .andExpect(jsonPath("$.unmatchedCount").value(1))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }
}
