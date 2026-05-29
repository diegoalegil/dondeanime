package com.dondeanime.backend.curated;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dondeanime.backend.admin.auth.AdminJwtService;
import com.dondeanime.backend.config.SecurityConfig;

@WebMvcTest(CuratedListTrackingController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "admin.username=admin",
        "admin.password=secret",
        "admin.cors.allowed-origins=http://localhost:4321"
})
class CuratedListTrackingControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CuratedListTrackingService service;

    @MockitoBean
    private AdminJwtService adminJwtService;

    @Test
    void tracksListViewWithoutAuth() throws Exception {
        mvc.perform(post("/api/track/lists/view")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"listSlug":"anime-para-empezar"}
                                """))
                .andExpect(status().isNoContent());

        verify(service).track(
                new CuratedListMetricRequest("anime-para-empezar", null),
                CuratedListMetricType.VIEW);
    }

    @Test
    void tracksAnimeClickWithoutAuth() throws Exception {
        mvc.perform(post("/api/track/lists/anime-click")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"listSlug":"anime-para-empezar","animeSlug":"frieren-beyond-journeys-end"}
                                """))
                .andExpect(status().isNoContent());

        verify(service).track(
                new CuratedListMetricRequest("anime-para-empezar", "frieren-beyond-journeys-end"),
                CuratedListMetricType.ANIME_CLICK);
    }
}
