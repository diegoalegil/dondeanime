package com.dondeanime.backend.anime.tmdb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP contra la API REST de TMDb.
 *
 * Autenticación: token v4 (JWT que empieza con "eyJ...") enviado
 * como Bearer en cada request. Se inyecta vía @Value desde la
 * property tmdb.api-key (que a su vez lee TMDB_API_KEY del .env).
 *
 * Dos métodos públicos:
 *  - searchTv(query): busca series por título.
 *  - getWatchProviders(tmdbId): devuelve las plataformas por país.
 */
@Component
public class TmdbClient {

    private static final String BASE_URL = "https://api.themoviedb.org/3";
    private static final String IMAGE_BASE = "https://image.tmdb.org/t/p/original";

    private final RestClient restClient;

    public TmdbClient(RestClient.Builder builder, @Value("${tmdb.api-key}") String apiKey) {
        this.restClient = builder
                .baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Busca series de TV por título. Lenguaje fijado a es-ES para que
     * los títulos vuelvan en español cuando exista localización.
     */
    public TmdbSearchResponse searchTv(String query) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/tv")
                        .queryParam("query", query)
                        .queryParam("language", "es-ES")
                        .queryParam("include_adult", false)
                        .build())
                .retrieve()
                .body(TmdbSearchResponse.class);
    }

    /**
     * Devuelve los watch providers de una serie por país.
     * El Map devuelto tiene como clave el código ISO de país.
     */
    public TmdbProvidersResponse getWatchProviders(Long tmdbId) {
        return restClient.get()
                .uri("/tv/{id}/watch/providers", tmdbId)
                .retrieve()
                .body(TmdbProvidersResponse.class);
    }

    /** Convierte un logo_path relativo de TMDb a URL absoluta. */
    public static String fullLogoUrl(String logoPath) {
        if (logoPath == null || logoPath.isBlank()) return null;
        return IMAGE_BASE + logoPath;
    }
}
