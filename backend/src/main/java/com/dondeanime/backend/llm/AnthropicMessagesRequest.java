package com.dondeanime.backend.llm;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.JsonNode;

/**
 * Cuerpo de POST /v1/messages (Anthropic API). Solo los campos que usamos.
 * Los nombres van en snake_case vía @JsonProperty; los null se omiten para
 * no enviar output_config cuando se pide texto libre.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record AnthropicMessagesRequest(
        String model,
        @JsonProperty("max_tokens") int maxTokens,
        String system,
        List<Message> messages,
        @JsonProperty("output_config") OutputConfig outputConfig) {

    record Message(String role, String content) {
    }

    record OutputConfig(Format format) {
    }

    /** Para salida estructurada el type es siempre "json_schema". */
    record Format(String type, JsonNode schema) {

        static Format jsonSchema(JsonNode schema) {
            return new Format("json_schema", schema);
        }
    }

    static AnthropicMessagesRequest from(String model, LlmRequest request) {
        OutputConfig outputConfig = request.outputSchema() == null
                ? null
                : new OutputConfig(Format.jsonSchema(request.outputSchema()));
        return new AnthropicMessagesRequest(
                model,
                request.maxOutputTokens(),
                request.system(),
                List.of(new Message("user", request.user())),
                outputConfig);
    }
}
