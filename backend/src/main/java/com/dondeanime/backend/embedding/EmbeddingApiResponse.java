package com.dondeanime.backend.embedding;

import java.util.List;

record EmbeddingApiResponse(
        List<EmbeddingApiData> data,
        String model) {
}
