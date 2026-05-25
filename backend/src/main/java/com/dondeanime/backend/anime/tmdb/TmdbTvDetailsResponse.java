package com.dondeanime.backend.anime.tmdb;

/**
 * Respuesta mínima de GET /tv/{id}. Para el hotfix solo necesitamos
 * la sinopsis localizada que TMDb devuelve en overview.
 */
public record TmdbTvDetailsResponse(
        String overview
) {}
