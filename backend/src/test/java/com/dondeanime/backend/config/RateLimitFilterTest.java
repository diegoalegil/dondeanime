package com.dondeanime.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

class RateLimitFilterTest {

    @Test
    void searchAllowsThirtyRequestsPerMinutePerIpAndRejectsThirtyFirst() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();

        RateLimitResult result = exhaust(filter, "/api/search", "203.0.113.10", 30);

        assertThat(result.passed()).isEqualTo(30);
        assertThat(result.rejected().getStatus()).isEqualTo(429);
        assertThat(result.rejected().getHeader(HttpHeaders.RETRY_AFTER)).isNotBlank();
        assertThat(result.rejected().getContentAsString()).contains("rate_limit_exceeded");
    }

    @Test
    void affiliateTrackingAllowsSixtyRequestsPerMinutePerIpAndRejectsSixtyFirst() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();

        RateLimitResult result = exhaust(filter, "/api/track/affiliate", "203.0.113.20", 60);

        assertThat(result.passed()).isEqualTo(60);
        assertThat(result.rejected().getStatus()).isEqualTo(429);
        assertThat(result.rejected().getHeader(HttpHeaders.RETRY_AFTER)).isNotBlank();
    }

    @Test
    void adminAllowsTenRequestsPerMinutePerIpAndRejectsEleventh() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();

        RateLimitResult result = exhaust(filter, "/api/admin/anime/attack-on-titan/overrides", "203.0.113.30", 10);

        assertThat(result.passed()).isEqualTo(10);
        assertThat(result.rejected().getStatus()).isEqualTo(429);
        assertThat(result.rejected().getHeader(HttpHeaders.RETRY_AFTER)).isNotBlank();
    }

    @Test
    void traktOAuthAllowsTwentyRequestsPerMinutePerIpAndRejectsTwentyFirst() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();

        RateLimitResult result = exhaust(filter, "/api/trakt/oauth/start", "203.0.113.40", 20);

        assertThat(result.passed()).isEqualTo(20);
        assertThat(result.rejected().getStatus()).isEqualTo(429);
        assertThat(result.rejected().getHeader(HttpHeaders.RETRY_AFTER)).isNotBlank();
    }

    @Test
    void traktSyncAllowsTenRequestsPerMinutePerIpAndRejectsEleventh() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();

        RateLimitResult result = exhaust(filter, "/api/trakt/sync", "203.0.113.50", 10);

        assertThat(result.passed()).isEqualTo(10);
        assertThat(result.rejected().getStatus()).isEqualTo(429);
        assertThat(result.rejected().getHeader(HttpHeaders.RETRY_AFTER)).isNotBlank();
    }

    private static RateLimitResult exhaust(
            RateLimitFilter filter,
            String path,
            String ip,
            int allowedRequests) throws Exception {
        AtomicInteger passed = new AtomicInteger();
        FilterChain chain = (request, response) -> passed.incrementAndGet();

        for (int i = 0; i < allowedRequests; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request(path, ip), response, chain);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse rejected = new MockHttpServletResponse();
        filter.doFilter(request(path, ip), rejected, chain);
        return new RateLimitResult(passed.get(), rejected);
    }

    private static MockHttpServletRequest request(String path, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        request.addHeader("X-Forwarded-For", ip);
        return request;
    }

    private record RateLimitResult(int passed, MockHttpServletResponse rejected) {}
}
