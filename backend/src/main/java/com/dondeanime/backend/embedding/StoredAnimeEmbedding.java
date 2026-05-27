package com.dondeanime.backend.embedding;

import java.time.Instant;
import java.util.List;

public record StoredAnimeEmbedding(
        Long animeId,
        String model,
        String contentHash,
        List<Double> embedding,
        Instant updatedAt) {
}
