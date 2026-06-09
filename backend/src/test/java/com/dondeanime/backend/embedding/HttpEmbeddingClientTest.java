package com.dondeanime.backend.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpEmbeddingClientTest {

    @Test
    void embedsInputThroughConfiguredApi() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpEmbeddingClient client = new HttpEmbeddingClient(
                builder,
                "https://embeddings.example.com",
                "secret",
                "embedding-model-small");

        server.expect(once(), requestTo("https://embeddings.example.com/v1/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer secret"))
                .andExpect(jsonPath("$.model").value("embedding-model-small"))
                .andExpect(jsonPath("$.input").value("quiero anime oscuro"))
                .andRespond(withSuccess("""
                        {
                          "data": [
                            {"index": 0, "embedding": [0.1, -0.2, 0.3]}
                          ],
                          "model": "embedding-model-small"
                        }
                        """, MediaType.APPLICATION_JSON));

        EmbeddingVector vector = client.embed("quiero anime oscuro");

        assertThat(vector.model()).isEqualTo("embedding-model-small");
        assertThat(vector.values()).containsExactly(0.1, -0.2, 0.3);
        server.verify();
    }

    @Test
    void blankInputIsRejectedBeforeHttpCall() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpEmbeddingClient client = new HttpEmbeddingClient(
                builder,
                "https://embeddings.example.com",
                "secret",
                "embedding-model-small");

        assertThatThrownBy(() -> client.embed(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("input");
        server.verify();
    }
}
