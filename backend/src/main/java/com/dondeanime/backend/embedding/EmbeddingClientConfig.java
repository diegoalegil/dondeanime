package com.dondeanime.backend.embedding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class EmbeddingClientConfig {

    @Bean
    EmbeddingClient embeddingClient(
            RestClient.Builder builder,
            @Value("${embeddings.enabled:false}") boolean enabled,
            @Value("${embeddings.model:}") String model,
            @Value("${embeddings.api-base:}") String apiBase,
            @Value("${embeddings.api-key:}") String apiKey) {
        if (!enabled) {
            return new DisabledEmbeddingClient(model);
        }
        return new HttpEmbeddingClient(builder, apiBase, apiKey, model);
    }
}
