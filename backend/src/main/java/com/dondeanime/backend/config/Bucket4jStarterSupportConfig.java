package com.dondeanime.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.giffing.bucket4j.spring.boot.starter.config.cache.SyncCacheResolver;

@Configuration(proxyBeanMethods = false)
class Bucket4jStarterSupportConfig {

    @Bean
    SyncCacheResolver bucket4jStarterSyncCacheResolver() {
        // The starter requires a cache resolver at startup even when limits are enforced by RateLimitFilter.
        return ignoredCacheName -> (key, tokens, shouldConsume, bucketConfiguration, listener, evictionSeconds, strategy) -> {
            throw new UnsupportedOperationException("Bucket4j starter filters are not configured");
        };
    }
}
