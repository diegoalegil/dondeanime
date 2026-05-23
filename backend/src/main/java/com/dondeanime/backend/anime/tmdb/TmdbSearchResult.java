package com.dondeanime.backend.anime.tmdb;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Un resultado de la búsqueda /search/tv.
 * Solo extraemos los campos relevantes; Jackson ignora el resto.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TmdbSearchResult(
        Long id,
        String name,
        String originalName,
        String overview,
        String firstAirDate,
        List<String> originCountry,
        String posterPath
) {}
