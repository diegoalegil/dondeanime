package com.dondeanime.backend.embedding;

import java.util.List;

public record EmbeddingVector(
        String model,
        List<Double> values) {
}
