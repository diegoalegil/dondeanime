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

class OpenAiEmbeddingClientTest {

    @Test
    void embedsInputThroughOpenAiApi() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiEmbeddingClient client = new OpenAiEmbeddingClient(
                builder,
                "https://api.openai.com",
                "sk_test",
                "text-embedding-3-small");

        server.expect(once(), requestTo("https://api.openai.com/v1/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer sk_test"))
                .andExpect(jsonPath("$.model").value("text-embedding-3-small"))
                .andExpect(jsonPath("$.input").value("quiero anime oscuro"))
                .andRespond(withSuccess("""
                        {
                          "data": [
                            {"index": 0, "embedding": [0.1, -0.2, 0.3]}
                          ],
                          "model": "text-embedding-3-small"
                        }
                        """, MediaType.APPLICATION_JSON));

        EmbeddingVector vector = client.embed("quiero anime oscuro");

        assertThat(vector.model()).isEqualTo("text-embedding-3-small");
        assertThat(vector.values()).containsExactly(0.1, -0.2, 0.3);
        server.verify();
    }

    @Test
    void blankInputIsRejectedBeforeHttpCall() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiEmbeddingClient client = new OpenAiEmbeddingClient(
                builder,
                "https://api.openai.com",
                "sk_test",
                "text-embedding-3-small");

        assertThatThrownBy(() -> client.embed(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("input");
        server.verify();
    }
}
