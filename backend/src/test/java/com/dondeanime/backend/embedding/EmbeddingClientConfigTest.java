package com.dondeanime.backend.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class EmbeddingClientConfigTest {

    @Test
    void disabledConfigReturnsDisabledClientWithoutApiKey() {
        EmbeddingClient client = new EmbeddingClientConfig().embeddingClient(
                RestClient.builder(),
                false,
                "text-embedding-3-small",
                "https://api.openai.com",
                "");

        assertThat(client.model()).isEqualTo("text-embedding-3-small");
        assertThatThrownBy(() -> client.embed("hola"))
                .isInstanceOf(EmbeddingClientDisabledException.class);
    }

    @Test
    void enabledConfigRequiresApiKey() {
        assertThatThrownBy(() -> new EmbeddingClientConfig().embeddingClient(
                RestClient.builder(),
                true,
                "text-embedding-3-small",
                "https://api.openai.com",
                " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("api-key");
    }
}
