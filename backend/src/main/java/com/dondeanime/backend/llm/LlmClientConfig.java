package com.dondeanime.backend.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class LlmClientConfig {

    @Bean
    LlmClient llmClient(
            RestClient.Builder builder,
            @Value("${llm.enabled:false}") boolean enabled,
            @Value("${llm.model:}") String model,
            @Value("${llm.api-base:}") String apiBase,
            @Value("${llm.api-key:}") String apiKey) {
        if (!enabled) {
            return new DisabledLlmClient(model);
        }
        return new AnthropicClient(builder, apiBase, apiKey, model);
    }
}
