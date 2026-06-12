package com.dondeanime.backend.llm;

/**
 * Cliente de modelos de lenguaje (proveedor: Anthropic API). Mismo patrón
 * desconectable que {@code EmbeddingClient}: con el flag apagado se inyecta
 * la variante Disabled y nada del flujo normal cambia.
 */
public interface LlmClient {

    String model();

    /**
     * Permite a los llamadores decidir sin control de flujo por excepciones
     * (p. ej. el procesado de noticias salta la ruta LLM si está apagado).
     */
    default boolean enabled() {
        return true;
    }

    LlmCompletion complete(LlmRequest request);
}
