package com.dondeanime.backend.anime;

/**
 * Temporada agregada: year + season + count.
 * El frontend la usa para listar "/temporada/2024/winter", etc.
 */
public record SeasonSummaryDto(
        int year,
        String season,
        long animeCount
) {}
