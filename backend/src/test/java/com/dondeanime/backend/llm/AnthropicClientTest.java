package com.dondeanime.backend.llm;

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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class AnthropicClientTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static AnthropicClient clientFor(RestClient.Builder builder) {
        return new AnthropicClient(
                builder,
                "https://llm.example.com",
                "secret",
                "claude-haiku-4-5");
    }

    @Test
    void completesWithStructuredOutputSchema() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AnthropicClient client = clientFor(builder);

        JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"titulo":{"type":"string"}},
                 "required":["titulo"],"additionalProperties":false}
                """);

        server.expect(once(), requestTo("https://llm.example.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "secret"))
                .andExpect(header("anthropic-version", "2023-06-01"))
                .andExpect(jsonPath("$.model").value("claude-haiku-4-5"))
                .andExpect(jsonPath("$.max_tokens").value(1500))
                .andExpect(jsonPath("$.system").value("Eres redactor"))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[0].content").value("Titulo original: X"))
                .andExpect(jsonPath("$.output_config.format.type").value("json_schema"))
                .andExpect(jsonPath("$.output_config.format.schema.type").value("object"))
                .andRespond(withSuccess("""
                        {
                          "content": [{"type": "text", "text": "{\\"titulo\\":\\"X en espanol\\"}"}],
                          "stop_reason": "end_turn",
                          "usage": {"input_tokens": 800, "output_tokens": 120}
                        }
                        """, MediaType.APPLICATION_JSON));

        LlmCompletion completion = client.complete(
                new LlmRequest("Eres redactor", "Titulo original: X", 1500, schema));

        assertThat(completion.text()).isEqualTo("{\"titulo\":\"X en espanol\"}");
        assertThat(completion.inputTokens()).isEqualTo(800);
        assertThat(completion.outputTokens()).isEqualTo(120);
        assertThat(completion.totalTokens()).isEqualTo(920);
        server.verify();
    }

    @Test
    void omitsOutputConfigForFreeText() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AnthropicClient client = clientFor(builder);

        server.expect(once(), requestTo("https://llm.example.com/v1/messages"))
                .andExpect(jsonPath("$.output_config").doesNotExist())
                .andExpect(jsonPath("$.system").doesNotExist())
                .andRespond(withSuccess("""
                        {
                          "content": [{"type": "text", "text": "hola"}],
                          "stop_reason": "end_turn",
                          "usage": {"input_tokens": 10, "output_tokens": 2}
                        }
                        """, MediaType.APPLICATION_JSON));

        LlmCompletion completion = client.complete(new LlmRequest(null, "saluda", 100, null));

        assertThat(completion.text()).isEqualTo("hola");
        server.verify();
    }

    @Test
    void truncatedResponseByMaxTokensFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AnthropicClient client = clientFor(builder);

        server.expect(once(), requestTo("https://llm.example.com/v1/messages"))
                .andRespond(withSuccess("""
                        {
                          "content": [{"type": "text", "text": "{\\"titulo\\":\\"a med"}],
                          "stop_reason": "max_tokens",
                          "usage": {"input_tokens": 800, "output_tokens": 1500}
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.complete(new LlmRequest(null, "hola", 1500, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("max_tokens");
        server.verify();
    }

    @Test
    void emptyContentFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AnthropicClient client = clientFor(builder);

        server.expect(once(), requestTo("https://llm.example.com/v1/messages"))
                .andRespond(withSuccess("""
                        {"content": [], "stop_reason": "end_turn"}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.complete(new LlmRequest(null, "hola", 100, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("contenido");
        server.verify();
    }

    @Test
    void blankUserIsRejectedBeforeHttpCall() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AnthropicClient client = clientFor(builder);

        assertThatThrownBy(() -> client.complete(new LlmRequest(null, " ", 100, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user");
        server.verify();
    }
}
