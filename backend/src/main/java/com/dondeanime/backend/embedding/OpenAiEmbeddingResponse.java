package com.dondeanime.backend.embedding;

import java.util.List;

record OpenAiEmbeddingResponse(
        List<OpenAiEmbeddingData> data,
        String model) {
}
