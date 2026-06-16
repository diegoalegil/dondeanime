package com.dondeanime.backend.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import io.github.diegoalegil.tsunagi.tmdb.TmdbCountryProviders;
import io.github.diegoalegil.tsunagi.tmdb.TmdbProvider;
import io.github.diegoalegil.tsunagi.tmdb.TmdbProvidersResponse;

class MovieWatchProviderClientTest {

    @Test
    void mapsMovieWatchProvidersResponseToTsunagiRecord() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MovieWatchProviderClient client = new MovieWatchProviderClient(
                builder, "https://api.themoviedb.org/3", "secret");

        server.expect(requestTo("https://api.themoviedb.org/3/movie/872585/watch/providers"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer secret"))
                .andRespond(withSuccess("""
                        {
                          "id": 872585,
                          "results": {
                            "ES": {
                              "link": "https://www.themoviedb.org/movie/872585/watch?locale=ES",
                              "flatrate": [
                                {"provider_id": 8, "provider_name": "Netflix", "logo_path": "/n.jpg", "display_priority": 0}
                              ],
                              "rent": [
                                {"provider_id": 2, "provider_name": "Apple TV", "logo_path": "/a.jpg", "display_priority": 3}
                              ],
                              "buy": [
                                {"provider_id": 2, "provider_name": "Apple TV", "logo_path": "/a.jpg", "display_priority": 3}
                              ]
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        TmdbProvidersResponse resp = client.getMovieWatchProviders(872585L);

        assertThat(resp.id()).isEqualTo(872585L);
        assertThat(resp.results()).containsKey("ES");
        TmdbCountryProviders es = resp.results().get("ES");
        assertThat(es.flatrate()).extracting(TmdbProvider::providerName).containsExactly("Netflix");
        assertThat(es.rent()).extracting(TmdbProvider::providerId).containsExactly(2);
        assertThat(es.buy()).extracting(TmdbProvider::providerName).containsExactly("Apple TV");
        // El JSON no trae "free": debe quedar null (no lista vacía) para que
        // ProviderSyncService.buildProviders lo trate igual que el cliente de TV.
        assertThat(es.free()).isNull();
        server.verify();
    }

    @Test
    void returnsEmptyResultsWhenTmdbHasNoProviders() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MovieWatchProviderClient client = new MovieWatchProviderClient(
                builder, "https://api.themoviedb.org/3", "secret");

        server.expect(requestTo("https://api.themoviedb.org/3/movie/1/watch/providers"))
                .andRespond(withSuccess("{\"id\":1,\"results\":{}}", MediaType.APPLICATION_JSON));

        TmdbProvidersResponse resp = client.getMovieWatchProviders(1L);

        assertThat(resp.id()).isEqualTo(1L);
        assertThat(resp.results()).isEmpty();
    }
}
