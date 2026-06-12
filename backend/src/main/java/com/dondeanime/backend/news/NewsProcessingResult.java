package com.dondeanime.backend.news;

/** Resultado de una pasada de procesado editorial sobre borradores RSS. */
public record NewsProcessingResult(
        boolean enabled,
        int draftsSeen,
        int itemsProcessed,
        int itemsPublished,
        int animeMatched,
        int itemsSkipped,
        int llmProcessed,
        int llmFailed,
        int sentToReview) {

    /** Pasada sin actividad (flag apagado o sin borradores). */
    static NewsProcessingResult empty(boolean enabled) {
        return new NewsProcessingResult(enabled, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
