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
                "",
                "",
                "");

        assertThat(client.model()).isEqualTo("disabled");
        assertThatThrownBy(() -> client.embed("hola"))
                .isInstanceOf(EmbeddingClientDisabledException.class);
    }

    @Test
    void enabledConfigRequiresApiKey() {
        assertThatThrownBy(() -> new EmbeddingClientConfig().embeddingClient(
                RestClient.builder(),
                true,
                "embedding-model-small",
                "https://embeddings.example.com",
                " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("api-key");
    }

    @Test
    void enabledConfigRequiresApiBase() {
        assertThatThrownBy(() -> new EmbeddingClientConfig().embeddingClient(
                RestClient.builder(),
                true,
                "embedding-model-small",
                " ",
                "secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("api-base");
    }
}
