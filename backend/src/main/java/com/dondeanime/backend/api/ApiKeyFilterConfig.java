package com.dondeanime.backend.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiKeyFilterConfig {

    @Bean
    ApiKeyAuthenticationFilter apiKeyAuthenticationFilter(ApiKeyService apiKeyService) {
        return new ApiKeyAuthenticationFilter(apiKeyService);
    }
}
