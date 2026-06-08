package com.dondeanime.backend.news;

/** Resultado de una pasada de procesado editorial sobre borradores RSS. */
public record NewsProcessingResult(
        boolean enabled,
        int draftsSeen,
        int itemsProcessed,
        int itemsPublished,
        int animeMatched,
        int itemsSkipped) {
}
