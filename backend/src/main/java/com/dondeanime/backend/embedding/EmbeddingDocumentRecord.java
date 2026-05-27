package com.dondeanime.backend.embedding;

public record EmbeddingDocumentRecord(
        Long animeId,
        AnimeSearchDocument document) {
}
