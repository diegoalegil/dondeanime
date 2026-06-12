package com.dondeanime.backend.llm;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Respuesta de POST /v1/messages. Solo los campos que usamos. */
@JsonIgnoreProperties(ignoreUnknown = true)
record AnthropicMessagesResponse(
        List<ContentBlock> content,
        @JsonProperty("stop_reason") String stopReason,
        Usage usage) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ContentBlock(String type, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Usage(
            @JsonProperty("input_tokens") Integer inputTokens,
            @JsonProperty("output_tokens") Integer outputTokens) {
    }
}
