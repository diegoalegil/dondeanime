package com.dondeanime.backend.anime.tmdb;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Video asociado a una serie en TMDb.
 *
 * Para trailers públicos nos interesan solo key, site y type:
 * key es el id de YouTube cuando site="YouTube".
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TmdbVideo(
        String key,
        String site,
        String type,
        String name
) {}
