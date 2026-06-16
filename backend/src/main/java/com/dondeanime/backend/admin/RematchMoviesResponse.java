package com.dondeanime.backend.admin;

/**
 * Resultado del re-match masivo de películas: cuántas cambiaron de tmdbId.
 */
public record RematchMoviesResponse(
        int updated
) {
}
