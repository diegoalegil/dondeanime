package com.dondeanime.backend.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LegacyApiRedirectControllerTest {

    private final LegacyApiRedirectController filter = new LegacyApiRedirectController();

    @Test
    void addsDeprecationHeadersToLegacyAnimeList() throws Exception {
        MockHttpServletResponse response = doFilter("/api/anime");

        assertThat(response.getHeader("Deprecation")).isEqualTo("true");
        assertThat(response.getHeader("Sunset")).isEqualTo(LegacyApiRedirectController.SUNSET_DATE);
        assertThat(response.getHeader("Link"))
                .isEqualTo("</docs/api-versioning.md>; rel=\"deprecation\"; type=\"text/markdown\"");
    }

    @Test
    void addsDeprecationHeadersToLegacyProviderRoute() throws Exception {
        MockHttpServletResponse response = doFilter("/api/providers/crunchyroll/ES");

        assertThat(response.getHeader("Deprecation")).isEqualTo("true");
    }

    @Test
    void skipsMaintenanceRoutes() throws Exception {
        MockHttpServletResponse response = doFilter("/api/anime/sync");

        assertThat(response.getHeader("Deprecation")).isNull();
    }

    @Test
    void skipsVersionedApi() throws Exception {
        MockHttpServletResponse response = doFilter("/api/v1/anime");

        assertThat(response.getHeader("Deprecation")).isNull();
    }

    private MockHttpServletResponse doFilter(String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
