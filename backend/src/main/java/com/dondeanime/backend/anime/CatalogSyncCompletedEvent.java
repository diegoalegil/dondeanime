package com.dondeanime.backend.anime;

/**
 * Se publica al terminar un sync de catálogo con al menos un anime guardado
 * (job nocturno o POST /api/anime/sync). Lo escucha RecommendationService
 * para invalidar su caché de similares, que si no serviría datos de ayer
 * durante hasta 24h.
 */
public record CatalogSyncCompletedEvent(int syncedCount) {
}
