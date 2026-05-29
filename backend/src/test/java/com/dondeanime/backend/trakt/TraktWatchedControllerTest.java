package com.dondeanime.backend.trakt;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TraktWatchedController.class)
class TraktWatchedControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TraktWatchedService traktWatchedService;

    @Test
    void returnsWatchedSlugsWithoutInternalIds() throws Exception {
        when(traktWatchedService.watched("user-123"))
                .thenReturn(new TraktWatchedResponse(List.of("attack-on-titan")));

        mvc.perform(get("/api/trakt/watched").queryParam("externalUserId", "user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slugs[0]").value("attack-on-titan"))
                .andExpect(jsonPath("$.id").doesNotExist());
    }
}
