package com.dondeanime.backend.trakt;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TraktHistoryClient {

    private static final String TRAKT_API_VERSION = "2";

    private final RestClient restClient;
    private final String clientId;

    public TraktHistoryClient(
            RestClient.Builder restClientBuilder,
            @Value("${trakt.api-base:https://api.trakt.tv}") String apiBase,
            @Value("${trakt.client-id:}") String clientId) {
        this.restClient = restClientBuilder.baseUrl(apiBase).build();
        this.clientId = clientId;
    }

    public List<TraktWatchedShow> fetchWatchedShows(String accessToken) {
        return restClient.get()
                .uri("/sync/watched/shows")
                .headers(headers -> applyHeaders(headers, accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<TraktRatedShow> fetchRatedShows(String accessToken) {
        return restClient.get()
                .uri("/sync/ratings/shows")
                .headers(headers -> applyHeaders(headers, accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    private void applyHeaders(HttpHeaders headers, String accessToken) {
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("trakt-api-version", TRAKT_API_VERSION);
        headers.set("trakt-api-key", clientId);
    }
}
