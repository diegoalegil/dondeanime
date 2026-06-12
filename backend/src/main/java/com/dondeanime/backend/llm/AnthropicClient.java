package com.dondeanime.backend.llm;

import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP de la Anthropic API (POST /v1/messages). Un solo endpoint sin
 * streaming, así que se usa RestClient directo (patrón HttpEmbeddingClient)
 * en vez de arrastrar el SDK oficial como dependencia.
 */
class AnthropicClient implements LlmClient {

    /** Versión de la API requerida en cada petición. */
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient restClient;
    private final String model;

    AnthropicClient(
            RestClient.Builder builder,
            String apiBase,
            String apiKey,
            String model) {
        this.model = required(model, "llm.model");
        this.restClient = builder
                .baseUrl(required(apiBase, "llm.api-base"))
                .defaultHeader("x-api-key", required(apiKey, "llm.api-key"))
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public LlmCompletion complete(LlmRequest request) {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new IllegalArgumentException("user no puede estar vacio");
        }

        AnthropicMessagesResponse response = restClient.post()
                .uri("/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .body(AnthropicMessagesRequest.from(model, request))
                .retrieve()
                .body(AnthropicMessagesResponse.class);

        if (response == null || response.content() == null || response.content().isEmpty()) {
            throw new IllegalStateException("El proveedor LLM no devolvio contenido");
        }
        // Si se cortó por max_tokens la salida está truncada (JSON a medias):
        // mejor fallar y que el llamador reintente que guardar basura.
        if ("max_tokens".equals(response.stopReason())) {
            throw new IllegalStateException(
                    "El proveedor LLM corto la respuesta por max_tokens; sube llm.max-output-tokens");
        }

        String text = response.content().stream()
                .filter(block -> "text".equals(block.type()))
                .map(AnthropicMessagesResponse.ContentBlock::text)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("El proveedor LLM devolvio un texto vacio"));

        int inputTokens = Optional.ofNullable(response.usage())
                .map(AnthropicMessagesResponse.Usage::inputTokens).orElse(0);
        int outputTokens = Optional.ofNullable(response.usage())
                .map(AnthropicMessagesResponse.Usage::outputTokens).orElse(0);
        return new LlmCompletion(text, inputTokens, outputTokens);
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " no puede estar vacio");
        }
        return value.trim();
    }
}
