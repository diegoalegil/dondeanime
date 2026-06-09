package com.dondeanime.backend.embedding;

import java.util.List;

record EmbeddingApiData(
        Integer index,
        List<Double> embedding) {
}
