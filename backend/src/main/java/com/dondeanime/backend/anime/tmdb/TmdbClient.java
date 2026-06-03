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
 * Métodos públicos:
 *  - searchTv(query): busca series por título.
 *  - getWatchProviders(tmdbId): devuelve las plataformas por país.
 *  - getTrailers(tmdbId, language): devuelve vídeos localizados.
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
     * Busca series y películas a la vez por título. Cada resultado trae
     * {@code media_type} ("tv"/"movie"), lo que permite distinguir serie de
     * película al cruzar contra AniList. Lenguaje fijado a es-ES.
     */
    public TmdbSearchResponse searchMulti(String query) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/multi")
                        .queryParam("query", query)
                        .queryParam("language", "es-ES")
                        .queryParam("include_adult", false)
                        .build())
                .retrieve()
                .body(TmdbSearchResponse.class);
    }

    public TmdbTvDetailsResponse getTvDetails(Long tmdbId, String language) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tv/{id}")
                        .queryParam("language", language)
                        .build(tmdbId))
                .retrieve()
                .body(TmdbTvDetailsResponse.class);
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

    /**
     * Devuelve los vídeos de una serie en el idioma pedido.
     * Si se pasa "es", TMDb recibe "es-ES".
     */
    public TmdbVideosResponse getTrailers(Long tmdbId, String language) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tv/{id}/videos")
                        .queryParam("language", normalizeLanguage(language))
                        .build(tmdbId))
                .retrieve()
                .body(TmdbVideosResponse.class);
    }

    /** Convierte un logo_path relativo de TMDb a URL absoluta. */
    public static String fullLogoUrl(String logoPath) {
        if (logoPath == null || logoPath.isBlank()) return null;
        return IMAGE_BASE + logoPath;
    }

    private static String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "es-ES";
        }
        String trimmed = language.trim();
        if (trimmed.contains("-")) {
            return trimmed;
        }
        return "es".equalsIgnoreCase(trimmed) ? "es-ES" : trimmed;
    }
}
