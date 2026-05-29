package com.dondeanime.backend.embedding;

public record EmbeddingRebuildResponse(
        int documentsPrepared,
        int embeddingsUpdated,
        int embeddingsSkipped,
        String model) {
}
