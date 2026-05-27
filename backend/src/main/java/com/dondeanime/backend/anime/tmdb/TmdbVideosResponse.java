package com.dondeanime.backend.anime.tmdb;

import java.util.List;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Respuesta de GET /tv/{id}/videos.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TmdbVideosResponse(
        Long id,
        List<TmdbVideo> results
) {}
