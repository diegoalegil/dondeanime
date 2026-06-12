package com.dondeanime.backend.admin.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class AdminJwtAuthenticationFilterTest {

    private static final Instant NOW = Instant.parse("2026-06-12T12:00:00Z");

    private final AdminJwtService jwtService =
            new AdminJwtService("secret", Clock.fixed(NOW, ZoneOffset.UTC));
    private final AdminTokenRevocationService revocationService = mock(AdminTokenRevocationService.class);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenAuthenticatesWhenNotRevoked() throws Exception {
        String token = jwtService.createAdminSession().token();
        new AdminJwtAuthenticationFilter(jwtService, revocationService)
                .doFilter(adminRequest(token), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void revokedTokenIsRejectedEvenIfSignatureIsValid() throws Exception {
        String token = jwtService.createAdminSession().token();
        String jti = jwtService.validClaims(token).orElseThrow().jti();
        when(revocationService.isRevoked(jti)).thenReturn(true);

        new AdminJwtAuthenticationFilter(jwtService, revocationService)
                .doFilter(adminRequest(token), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void withoutRevocationServiceBehavesAsBefore() throws Exception {
        String token = jwtService.createAdminSession().token();

        new AdminJwtAuthenticationFilter(jwtService, null)
                .doFilter(adminRequest(token), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    private static MockHttpServletRequest adminRequest(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/dashboard");
        request.setRequestURI("/api/admin/dashboard");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
