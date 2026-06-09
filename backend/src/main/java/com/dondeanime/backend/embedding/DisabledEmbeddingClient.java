package com.dondeanime.backend.embedding;

class DisabledEmbeddingClient implements EmbeddingClient {

    private final String model;

    DisabledEmbeddingClient(String model) {
        this.model = model == null || model.isBlank() ? "disabled" : model.trim();
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
