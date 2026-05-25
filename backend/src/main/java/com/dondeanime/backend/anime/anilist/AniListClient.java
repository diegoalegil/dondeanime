package com.dondeanime.backend.anime.anilist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AniListClient {

    /**
     * AniList limita perPage a 50. Si pides más, devuelve 50 silenciosamente.
     * Para totales mayores hay que paginar (lo hace fetchPopular abajo).
     */
    private static final int MAX_PER_PAGE = 50;

    private static final String GRAPHQL_QUERY = """
        query ($page: Int, $perPage: Int) {
          Page(page: $page, perPage: $perPage) {
            media(type: ANIME, sort: POPULARITY_DESC) {
              id
              title { romaji english }
              startDate { year month day }
              endDate { year month day }
              episodes
              format
              status
              averageScore
              popularity
              description(asHtml: false)
              coverImage { large }
              bannerImage
              genres
              studios { nodes { id name isAnimationStudio } }
              season
              seasonYear
            }
          }
        }
        """;

    private final RestClient restClient;

    public AniListClient(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("https://graphql.anilist.co")
                .build();
    }

    /**
     * Devuelve los {@code totalCount} anime más populares paginando
     * internamente en bloques de {@value #MAX_PER_PAGE}.
     */
    public List<AniListMedia> fetchPopular(int totalCount) {
        int perPage = Math.min(MAX_PER_PAGE, totalCount);
        int pages = (int) Math.ceil(totalCount / (double) perPage);

        List<AniListMedia> all = new ArrayList<>(totalCount);
        for (int page = 1; page <= pages; page++) {
            List<AniListMedia> chunk = fetchPage(page, perPage);
            if (chunk.isEmpty()) break; // AniList se quedó sin resultados
            all.addAll(chunk);
            if (all.size() >= totalCount) break;
        }
        return all.size() > totalCount ? all.subList(0, totalCount) : all;
    }

    private List<AniListMedia> fetchPage(int page, int perPage) {
        Map<String, Object> body = Map.of(
                "query", GRAPHQL_QUERY,
                "variables", Map.of("page", page, "perPage", perPage)
        );

        AniListResponse response = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(AniListResponse.class);

        if (response == null || response.data() == null || response.data().page() == null) {
            return List.of();
        }
        return response.data().page().media();
    }
}
