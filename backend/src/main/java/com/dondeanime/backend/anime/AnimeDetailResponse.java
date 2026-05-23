package com.dondeanime.backend.anime;

import java.util.List;
import java.util.Map;

import com.dondeanime.backend.provider.WatchProvider;

/**
 * DTO de salida para GET /api/anime/{slug}.
 *
 * Envuelve el anime con sus WatchProvider agrupados por código de país.
 * Devolvemos la entidad Anime directamente por ahora; cuando expongamos
 * la API a frontend en mes 2 lo refactorizaremos a un DTO más limpio
 * (sin id interno, sin syncedAt, etc.).
 */
public record AnimeDetailResponse(
        Anime anime,
        Map<String, List<WatchProvider>> watchProvidersByCountry
) {}
