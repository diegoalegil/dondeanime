package com.dondeanime.backend.embedding;

class DisabledEmbeddingClient implements EmbeddingClient {

    private final String model;

    DisabledEmbeddingClient(String model) {
        this.model = model == null || model.isBlank() ? "text-embedding-3-small" : model.trim();
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public EmbeddingVector embed(String input) {
        throw new EmbeddingClientDisabledException();
    }
}
