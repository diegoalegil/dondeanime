package com.dondeanime.backend.anime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Temporada agregada: year + season + count.
 * El frontend la usa para listar "/temporada/2024/winter", etc.
 */
@Schema(description = "Temporada de estreno agregada con numero de anime")
public record SeasonSummaryDto(
        int year,
        String season,
        long animeCount
) {}
