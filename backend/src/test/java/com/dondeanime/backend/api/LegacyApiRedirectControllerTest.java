package com.dondeanime.backend.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.dondeanime.backend.config.SecurityConfig;

@WebMvcTest(LegacyApiRedirectController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "admin.username=admin",
        "admin.password=secret",
        "admin.cors.allowed-origins=http://localhost:4321"
})
class LegacyApiRedirectControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void redirectsLegacyAnimeListWithDeprecationHeaders() throws Exception {
        mvc.perform(get("/api/anime"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "http://localhost/api/v1/anime"))
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(header().string("Sunset", LegacyApiRedirectController.SUNSET_DATE));
    }

    @Test
    void redirectsLegacyRouteAndPreservesQueryString() throws Exception {
        mvc.perform(get("/api/providers?country=ES"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "http://localhost/api/v1/providers?country=ES"))
                .andExpect(header().string("Link", "</docs/api-versioning.md>; rel=\"deprecation\"; type=\"text/markdown\""));
    }

    @Test
    void redirectsLegacySitemap() throws Exception {
        mvc.perform(get("/api/sitemap"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "http://localhost/api/v1/sitemap"));
    }
}
