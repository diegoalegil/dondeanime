package com.dondeanime.backend.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.dondeanime.backend.admin.auth.AdminJwtAuthenticationFilter;
import com.dondeanime.backend.admin.auth.AdminJwtService;
import com.dondeanime.backend.admin.auth.AdminTokenRevocationService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    AdminJwtAuthenticationFilter adminJwtAuthenticationFilter(
            AdminJwtService adminJwtService,
            // ObjectProvider: en los slices @WebMvcTest el @Service no existe
            // y el filtro queda sin denylist (comportamiento previo); en la
            // app completa sí, y las sesiones revocadas dejan de valer.
            org.springframework.beans.factory.ObjectProvider<AdminTokenRevocationService> revocationProvider) {
        return new AdminJwtAuthenticationFilter(adminJwtService, revocationProvider.getIfAvailable());
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            AdminJwtAuthenticationFilter adminJwtAuthenticationFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admin/login").permitAll()
                        .requestMatchers("/api/admin/**").authenticated()
                        // Endpoints de mantenimiento del catálogo: disparan syncs masivos
                        // contra AniList/TMDb, así que exigen el mismo JWT admin. Las URLs
                        // se mantienen estables porque docs y scripts las referencian.
                        .requestMatchers(HttpMethod.POST,
                                "/api/anime/sync",
                                "/api/anime/match",
                                "/api/anime/sync-providers",
                                "/api/anime/sync-trailers").authenticated()
                        .anyRequest().permitAll())
                .httpBasic(AbstractHttpConfigurer::disable)
                .addFilterBefore(adminJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception.authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"unauthorized\"}");
                }))
                .build();
    }

    @Bean
    FilterRegistrationBean<AdminJwtAuthenticationFilter> adminJwtFilterRegistration(
            AdminJwtAuthenticationFilter adminJwtAuthenticationFilter) {
        FilterRegistrationBean<AdminJwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(adminJwtAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${admin.cors.allowed-origins}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(splitCsv(allowedOrigins));
        configuration.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-API-Key",
                RequestMdcFilter.REQUEST_ID_HEADER));
        configuration.setExposedHeaders(List.of(
                "WWW-Authenticate",
                "X-RateLimit-Limit",
                "X-RateLimit-Remaining",
                "Deprecation",
                "Sunset",
                "Link",
                RequestMdcFilter.REQUEST_ID_HEADER));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private static List<String> splitCsv(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }
}
