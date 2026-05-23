package com.dondeanime.backend.anime.tmdb;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Respuesta de GET /search/tv. TMDb usa snake_case en JSON;
 * @JsonNaming convierte camelCase de Java al snake_case del JSON
 * automáticamente para todos los campos del record.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TmdbSearchResponse(
        Integer page,
        List<TmdbSearchResult> results,
        Integer totalResults,
        Integer totalPages
) {}
