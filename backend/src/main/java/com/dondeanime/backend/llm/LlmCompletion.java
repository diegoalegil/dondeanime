package com.dondeanime.backend.llm;

/** Respuesta de un LLM: texto generado y tokens consumidos (para control de coste). */
public record LlmCompletion(
        String text,
        int inputTokens,
        int outputTokens) {

    public int totalTokens() {
        return inputTokens + outputTokens;
    }
}
