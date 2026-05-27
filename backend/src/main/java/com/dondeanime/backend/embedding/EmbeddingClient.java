package com.dondeanime.backend.embedding;

public interface EmbeddingClient {

    String model();

    EmbeddingVector embed(String input);
}
