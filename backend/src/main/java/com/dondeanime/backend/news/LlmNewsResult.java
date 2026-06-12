package com.dondeanime.backend.news;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Redacción en español que devuelve el LLM para un borrador de noticia. */
@JsonIgnoreProperties(ignoreUnknown = true)
record LlmNewsResult(
        String titulo,
        String resumen,
        String cuerpo,
        @JsonProperty("meta_titulo") String metaTitulo,
        @JsonProperty("meta_descripcion") String metaDescripcion) {
}
