package com.dondeanime.backend.embedding;

import java.util.List;

record OpenAiEmbeddingData(
        Integer index,
        List<Double> embedding) {
}
