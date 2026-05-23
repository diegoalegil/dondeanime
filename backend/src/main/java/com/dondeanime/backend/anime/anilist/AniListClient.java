package com.dondeanime.backend.anime.anilist;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AniListClient {

    private static final String GRAPHQL_QUERY = """
        query ($perPage: Int) {
          Page(page: 1, perPage: $perPage) {
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

    public List<AniListMedia> fetchPopular(int perPage) {
        Map<String, Object> body = Map.of(
                "query", GRAPHQL_QUERY,
                "variables", Map.of("perPage", perPage)
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