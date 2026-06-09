package com.dondeanime.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiKeyAuthenticationFilterTest {

    private final ApiKeyService apiKeyService = org.mockito.Mockito.mock(ApiKeyService.class);
    private final ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(apiKeyService);

    @Test
    void rejectsVersionedApiWithoutKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/anime");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("missing_api_key");
        verifyNoInteractions(apiKeyService);
    }

    @Test
    void rejectsVersionedApiWithInvalidKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/anime");
        request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, "da_free_missing");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(apiKeyService.findUsage("da_free_missing")).thenThrow(new ApiKeyNotFoundException());

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("invalid_api_key");
    }

    @Test
    void allowsDocsWithoutKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/docs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        verifyNoInteractions(apiKeyService);
    }

    @Test
    void allowsDocsSubtreeAndOptionsWithoutKey() throws Exception {
        MockHttpServletResponse docsResponse = new MockHttpServletResponse();
        MockHttpServletResponse optionsResponse = new MockHttpServletResponse();

        filter.doFilter(
                new MockHttpServletRequest("GET", "/api/v1/docs/openapi.yaml"),
                docsResponse,
                new MockFilterChain());
        filter.doFilter(
                new MockHttpServletRequest("OPTIONS", "/api/v1/anime"),
                optionsResponse,
                new MockFilterChain());

        assertThat(docsResponse.getStatus()).isEqualTo(200);
        assertThat(optionsResponse.getStatus()).isEqualTo(200);
        verifyNoInteractions(apiKeyService);
    }

    @Test
    void allowsLegacyApiWithoutKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/anime");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        verifyNoInteractions(apiKeyService);
    }

    @Test
    void recordsUsageAndAddsRateLimitHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/anime");
        request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, "da_free_test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(apiKeyService.findUsage("da_free_test"))
                .thenReturn(new ApiKeyUsage("FREE", 1_000, 0, 1_000));
        when(apiKeyService.recordUsage("da_free_test", "/api/v1/anime"))
                .thenReturn(new ApiKeyUsage("FREE", 1_000, 1, 999));

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader(ApiKeyAuthenticationFilter.RATE_LIMIT_LIMIT_HEADER)).isEqualTo("1000");
        assertThat(response.getHeader(ApiKeyAuthenticationFilter.RATE_LIMIT_REMAINING_HEADER)).isEqualTo("999");
        verify(apiKeyService).recordUsage("da_free_test", "/api/v1/anime");
    }

    @Test
    void rejectsMonthlyQuotaExceeded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/anime");
        request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, "da_free_test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(apiKeyService.findUsage("da_free_test"))
                .thenReturn(new ApiKeyUsage("FREE", 1_000, 999, 1));
        when(apiKeyService.recordUsage("da_free_test", "/api/v1/anime"))
                .thenThrow(new ApiQuotaExceededException());

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader(ApiKeyAuthenticationFilter.RATE_LIMIT_LIMIT_HEADER)).isEqualTo("1000");
        assertThat(response.getHeader(ApiKeyAuthenticationFilter.RATE_LIMIT_REMAINING_HEADER)).isEqualTo("0");
        assertThat(response.getContentAsString()).contains("monthly_quota_exceeded");
    }
}
