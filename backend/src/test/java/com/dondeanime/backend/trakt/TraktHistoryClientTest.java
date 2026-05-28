package com.dondeanime.backend.trakt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TraktHistoryClientTest {

    @Test
    void fetchesWatchedShowsWithTraktHeaders() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TraktHistoryClient client = new TraktHistoryClient(builder, "https://api.trakt.tv", "client-id");

        server.expect(once(), requestTo("https://api.trakt.tv/sync/watched/shows"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(header("trakt-api-version", "2"))
                .andExpect(header("trakt-api-key", "client-id"))
                .andRespond(withSuccess("""
                        [
                          {
                            "plays": 1,
                            "last_watched_at": "2026-05-20T19:00:00Z",
                            "show": {
                              "title": "Attack on Titan",
                              "year": 2013,
                              "ids": {"trakt": 1390, "slug": "attack-on-titan", "tmdb": 1429}
                            }
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<TraktWatchedShow> watched = client.fetchWatchedShows("access-token");

        assertThat(watched).hasSize(1);
        assertThat(watched.getFirst().show().title()).isEqualTo("Attack on Titan");
        assertThat(watched.getFirst().lastWatchedAt()).isEqualTo("2026-05-20T19:00:00Z");
        server.verify();
    }

    @Test
    void fetchesRatedShowsWithTraktHeaders() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TraktHistoryClient client = new TraktHistoryClient(builder, "https://api.trakt.tv", "client-id");

        server.expect(once(), requestTo("https://api.trakt.tv/sync/ratings/shows"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(header("trakt-api-version", "2"))
                .andExpect(header("trakt-api-key", "client-id"))
                .andRespond(withSuccess("""
                        [
                          {
                            "rating": 9,
                            "rated_at": "2026-05-22T19:00:00Z",
                            "show": {
                              "title": "Attack on Titan",
                              "year": 2013,
                              "ids": {"trakt": 1390, "slug": "attack-on-titan", "tmdb": 1429}
                            }
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<TraktRatedShow> rated = client.fetchRatedShows("access-token");

        assertThat(rated).hasSize(1);
        assertThat(rated.getFirst().rating()).isEqualTo(9);
        assertThat(rated.getFirst().ratedAt()).isEqualTo("2026-05-22T19:00:00Z");
        server.verify();
    }
}
