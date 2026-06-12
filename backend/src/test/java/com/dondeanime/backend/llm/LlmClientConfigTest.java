package com.dondeanime.backend.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class LlmClientConfigTest {

    @Test
    void disabledConfigReturnsDisabledClientWithoutApiKey() {
        LlmClient client = new LlmClientConfig().llmClient(
                RestClient.builder(),
                false,
                "",
                "",
                "");

        assertThat(client.model()).isEqualTo("disabled");
        assertThat(client.enabled()).isFalse();
        assertThatThrownBy(() -> client.complete(new LlmRequest(null, "hola", 100, null)))
                .isInstanceOf(LlmClientDisabledException.class);
    }

    @Test
    void disabledConfigKeepsConfiguredModelName() {
        LlmClient client = new LlmClientConfig().llmClient(
                RestClient.builder(),
                false,
                "claude-haiku-4-5",
                "",
                "");

        assertThat(client.model()).isEqualTo("claude-haiku-4-5");
        assertThat(client.enabled()).isFalse();
    }

    @Test
    void enabledConfigRequiresApiKey() {
        assertThatThrownBy(() -> new LlmClientConfig().llmClient(
                RestClient.builder(),
                true,
                "claude-haiku-4-5",
                "https://llm.example.com",
                " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("api-key");
    }

    @Test
    void enabledConfigRequiresApiBase() {
        assertThatThrownBy(() -> new LlmClientConfig().llmClient(
                RestClient.builder(),
                true,
                "claude-haiku-4-5",
                " ",
                "secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("api-base");
    }

    @Test
    void enabledConfigRequiresModel() {
        assertThatThrownBy(() -> new LlmClientConfig().llmClient(
                RestClient.builder(),
                true,
                " ",
                "https://llm.example.com",
                "secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("model");
    }
}
