package com.dondeanime.backend.admin.auth;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AdminJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Endpoints de mantenimiento fuera de /api/admin/ que también se protegen
     * con el JWT admin (SecurityConfig les exige authenticated()).
     */
    private static final Set<String> MAINTENANCE_PATHS = Set.of(
            "/api/anime/sync",
            "/api/anime/match",
            "/api/anime/sync-providers",
            "/api/anime/sync-trailers");

    private final AdminJwtService adminJwtService;

    public AdminJwtAuthenticationFilter(AdminJwtService adminJwtService) {
        this.adminJwtService = adminJwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!requiresAdminToken(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            String token = authorization.substring(BEARER_PREFIX.length());
            if (adminJwtService.isValidAdminToken(token)) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        "admin",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private static boolean requiresAdminToken(String path) {
        return (path.startsWith("/api/admin/") && !"/api/admin/login".equals(path))
                || MAINTENANCE_PATHS.contains(path);
    }
}
