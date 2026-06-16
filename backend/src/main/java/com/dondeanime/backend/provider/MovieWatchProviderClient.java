package com.dondeanime.backend.provider;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.diegoalegil.tsunagi.tmdb.TmdbCountryProviders;
import io.github.diegoalegil.tsunagi.tmdb.TmdbProvider;
import io.github.diegoalegil.tsunagi.tmdb.TmdbProvidersResponse;

/**
 * Consulta /movie/{id}/watch/providers de TMDb para los anime de formato MOVIE.
 *
 * El TmdbClient de Tsunagi solo expone /tv/{id}/watch/providers, así que las
 * películas (que el matcher resuelve con un id de TMDb de PELÍCULA vía
 * searchMulti + preferMovie) se quedaban sin datos de disponibilidad. Este
 * cliente cubre ese hueco con el mismo patrón de RestClient que AnthropicClient,
 * y devuelve el MISMO record {@link TmdbProvidersResponse} para reutilizar
 * {@code ProviderSyncService.buildProviders} sin cambios.
 *
 * Es ADITIVO y AISLADO: si TMDb cambia el formato o falla, las películas quedan
 * como hasta ahora (sin providers) y el try/catch de
 * {@code ProviderSyncService.syncAll} lo absorbe. No puede afectar al sync de
 * series.
 *
 * Auth TMDb: token v4 (JWT) en {@code Authorization: Bearer} (igual que el
 * TmdbClient de Tsunagi). La api-key se inyecta con default vacío para que el
 * bean construya también en tests que no la necesitan (solo se usa en películas).
 */
@Component
public class MovieWatchProviderClient {

    private final RestClient restClient;

    public MovieWatchProviderClient(
            RestClient.Builder builder,
            @Value("${tmdb.api-base:https://api.themoviedb.org/3}") String apiBase,
            @Value("${tmdb.api-key:}") String apiKey) {
        this.restClient = builder
                .baseUrl(apiBase)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public TmdbProvidersResponse getMovieWatchProviders(Long movieTmdbId) {
        Response resp = restClient.get()
                .uri("/movie/{id}/watch/providers", movieTmdbId)
                .retrieve()
                .body(Response.class);
        return toTsunagi(movieTmdbId, resp);
    }

    private static TmdbProvidersResponse toTsunagi(Long id, Response resp) {
        Map<String, TmdbCountryProviders> byCountry = new LinkedHashMap<>();
        if (resp != null && resp.results() != null) {
            resp.results().forEach((country, c) -> {
                if (c != null) {
                    byCountry.put(country, new TmdbCountryProviders(
                            c.link(),
                            map(c.flatrate()),
                            map(c.free()),
                            map(c.rent()),
                            map(c.buy())));
                }
            });
        }
        return new TmdbProvidersResponse(id, byCountry);
    }

    private static List<TmdbProvider> map(List<Provider> list) {
        if (list == null) {
            return null;
        }
        return list.stream()
                .map(p -> new TmdbProvider(p.providerId(), p.providerName(), p.logoPath(), p.displayPriority()))
                .toList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Response(@JsonProperty("results") Map<String, Country> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Country(
            @JsonProperty("link") String link,
            @JsonProperty("flatrate") List<Provider> flatrate,
            @JsonProperty("free") List<Provider> free,
            @JsonProperty("rent") List<Provider> rent,
            @JsonProperty("buy") List<Provider> buy) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Provider(
            @JsonProperty("provider_id") Integer providerId,
            @JsonProperty("provider_name") String providerName,
            @JsonProperty("logo_path") String logoPath,
            @JsonProperty("display_priority") Integer displayPriority) {
    }
}
