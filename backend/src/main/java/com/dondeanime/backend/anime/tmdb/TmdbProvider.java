package com.dondeanime.backend.anime.tmdb;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Un provider concreto (Crunchyroll, Netflix, ...).
 * logoPath es un path relativo, hay que prefijarlo con
 * https://image.tmdb.org/t/p/original para conseguir la URL final.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TmdbProvider(
        Integer providerId,
        String providerName,
        String logoPath,
        Integer displayPriority
) {}
