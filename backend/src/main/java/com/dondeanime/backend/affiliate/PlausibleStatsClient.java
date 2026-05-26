package com.dondeanime.backend.affiliate;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class PlausibleStatsClient {

    private static final Logger log = LoggerFactory.getLogger(PlausibleStatsClient.class);

    private final RestClient restClient;
    private final boolean enabled;
    private final String apiKey;
    private final String siteId;

    public PlausibleStatsClient(
            RestClient.Builder restClientBuilder,
            @Value("${plausible.api-base:https://plausible.io}") String apiBase,
            @Value("${plausible.api-key:}") String apiKey,
            @Value("${plausible.site-id:dondeanime.com}") String siteId,
            @Value("${plausible.enabled:false}") boolean enabled) {
        this.restClient = restClientBuilder.baseUrl(apiBase).build();
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.siteId = siteId;
    }

    public List<PlausiblePageMetricDto> topAnimePages30Days() {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            return List.of();
        }

        Map<String, Object> request = Map.of(
                "site_id", siteId,
                "metrics", List.of("visitors"),
                "date_range", "30d",
                "dimensions", List.of("event:page"),
                "filters", List.of(List.of("contains", "event:page", List.of("/anime/"))),
                "order_by", List.of(List.of("visitors", "desc")),
                "pagination", Map.of("limit", 10));

        try {
            PlausibleQueryResponse response = restClient.post()
                    .uri("/api/v2/query")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .body(PlausibleQueryResponse.class);

            if (response == null || response.results() == null) {
                return List.of();
            }

            return response.results().stream()
                    .filter(row -> row.dimensions() != null && !row.dimensions().isEmpty())
                    .filter(row -> row.metrics() != null && !row.metrics().isEmpty())
                    .map(row -> new PlausiblePageMetricDto(
                            row.dimensions().getFirst(),
                            row.metrics().getFirst().longValue()))
                    .toList();
        } catch (RestClientException e) {
            log.warn("No se pudo consultar Plausible Stats API: {}", e.getMessage());
            return List.of();
        }
    }

    public Long animeDetailPageviews30Days() {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            return 0L;
        }

        Map<String, Object> request = Map.of(
                "site_id", siteId,
                "metrics", List.of("pageviews"),
                "date_range", "30d",
                "filters", List.of(List.of("contains", "event:page", List.of("/anime/"))),
                "pagination", Map.of("limit", 1));

        try {
            PlausibleQueryResponse response = restClient.post()
                    .uri("/api/v2/query")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .body(PlausibleQueryResponse.class);

            if (response == null || response.results() == null || response.results().isEmpty()) {
                return 0L;
            }

            List<Number> metrics = response.results().getFirst().metrics();
            if (metrics == null || metrics.isEmpty()) {
                return 0L;
            }
            return metrics.getFirst().longValue();
        } catch (RestClientException e) {
            log.warn("No se pudo consultar Plausible Stats API: {}", e.getMessage());
            return 0L;
        }
    }
}
