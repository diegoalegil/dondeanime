package com.dondeanime.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

class RequestMdcFilterTest {

    private final RequestMdcFilter filter = new RequestMdcFilter();

    @Test
    void copiesRequestIdToMdcAndResponseHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/anime/attack-on-titan");
        request.addHeader(RequestMdcFilter.REQUEST_ID_HEADER, "req-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingFilterChain chain = new CapturingFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.traceId).isEqualTo("req-123");
        assertThat(chain.animeSlug).isEqualTo("attack-on-titan");
        assertThat(response.getHeader(RequestMdcFilter.REQUEST_ID_HEADER)).isEqualTo("req-123");
        assertThat(MDC.get("trace_id")).isNull();
        assertThat(MDC.get("anime_slug")).isNull();
    }

    @Test
    void generatesRequestIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/providers");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingFilterChain chain = new CapturingFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(UUID.fromString(chain.traceId)).isNotNull();
        assertThat(chain.animeSlug).isNull();
        assertThat(response.getHeader(RequestMdcFilter.REQUEST_ID_HEADER)).isEqualTo(chain.traceId);
        assertThat(MDC.get("trace_id")).isNull();
    }

    private static class CapturingFilterChain extends MockFilterChain {

        private String traceId;
        private String animeSlug;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
            this.traceId = MDC.get("trace_id");
            this.animeSlug = MDC.get("anime_slug");
        }
    }
}
