package com.dondeanime.backend.llm;

import tools.jackson.databind.JsonNode;

/**
 * Petición a un LLM.
 *
 * @param system          instrucciones de sistema (puede ser null)
 * @param user            mensaje del usuario (obligatorio)
 * @param maxOutputTokens tope de tokens de salida
 * @param outputSchema    JSON Schema para salida estructurada; null = texto libre.
 *                        Ojo: el schema no admite maxLength/minLength — los límites
 *                        de longitud se aplican truncando en Java.
 */
public record LlmRequest(
        String system,
        String user,
        int maxOutputTokens,
        JsonNode outputSchema) {
}
