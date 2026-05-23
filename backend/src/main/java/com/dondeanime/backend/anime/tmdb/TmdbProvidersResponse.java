package com.dondeanime.backend.anime.tmdb;

import java.util.Map;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Respuesta de GET /tv/{id}/watch/providers.
 *
 * El campo "results" es un Map donde la clave es el código de país
 * (ES, MX, AR, US, ...) y el valor son los providers en ese país.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TmdbProvidersResponse(
        Long id,
        Map<String, TmdbCountryProviders> results
) {}
