package com.dondeanime.backend.anime.tmdb;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Providers disponibles en un país concreto, agrupados por tipo:
 * - flatrate: incluido en suscripción (lo que más interesa)
 * - free: gratis con anuncios
 * - rent: alquiler
 * - buy: compra
 *
 * Cualquier lista puede ser null si no hay providers de ese tipo.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TmdbCountryProviders(
        String link,
        List<TmdbProvider> flatrate,
        List<TmdbProvider> free,
        List<TmdbProvider> rent,
        List<TmdbProvider> buy
) {}
