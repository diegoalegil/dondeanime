package com.dondeanime.backend.embedding;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

class HttpEmbeddingClient implements EmbeddingClient {

    private final RestClient restClient;
    private final String model;

    HttpEmbeddingClient(
            RestClient.Builder builder,
            String apiBase,
            String apiKey,
            String model) {
        this.model = required(model, "embeddings.model");
        this.restClient = builder
                .baseUrl(required(apiBase, "embeddings.api-base"))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + required(apiKey, "embeddings.api-key"))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public EmbeddingVector embed(String input) {
        String safeInput = required(input, "input");
        EmbeddingApiResponse response = restClient.post()
                .uri("/v1/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EmbeddingApiRequest(model, safeInput))
                .retrieve()
                .body(EmbeddingApiResponse.class);

        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new IllegalStateException("El proveedor de embeddings no devolvio datos");
        }

        List<Double> embedding = response.data().getFirst().embedding();
        if (embedding == null || embedding.isEmpty()) {
            throw new IllegalStateException("El proveedor de embeddings devolvio un vector vacio");
        }

        String responseModel = response.model() == null || response.model().isBlank()
                ? model
                : response.model().trim();
        return new EmbeddingVector(responseModel, embedding);
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " no puede estar vacio");
        }
        return value.trim();
    }
}
