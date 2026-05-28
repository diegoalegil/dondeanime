package com.dondeanime.backend.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public WebConfig(@Value("${public.cors.allowed-origins}") String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Authorization", "X-API-Key", RequestMdcFilter.REQUEST_ID_HEADER)
                .exposedHeaders(
                        "X-RateLimit-Limit",
                        "X-RateLimit-Remaining",
                        "Deprecation",
                        "Sunset",
                        "Link",
                        RequestMdcFilter.REQUEST_ID_HEADER)
                .maxAge(3600);
    }

    @Bean
    FilterRegistrationBean<RequestMdcFilter> requestMdcFilter() {
        FilterRegistrationBean<RequestMdcFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestMdcFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
