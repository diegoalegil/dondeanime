package com.dondeanime.backend.news;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dondeanime.backend.llm.LlmClient;
import com.dondeanime.backend.llm.LlmCompletion;
import com.dondeanime.backend.llm.LlmRequest;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Redacta la versión en español de un borrador RSS usando el LLM: título,
 * resumen, cuerpo y metas SEO. No publica ni decide estados — eso lo orquesta
 * {@link NewsProcessingService}. Si el LLM falla, devuelve false y el ítem
 * queda intacto (lo reintenta la siguiente pasada).
 */
@Service
public class LlmNewsProcessor {

    private static final Logger log = LoggerFactory.getLogger(LlmNewsProcessor.class);

    // Límites de columna de news_item; el schema del LLM no admite maxLength,
    // así que los límites van en las descriptions y se truncan aquí.
    private static final int MAX_TITLE = 200;
    private static final int MAX_SUMMARY = 500;
    private static final int MAX_META_TITLE = 70;
    private static final int MAX_META_DESCRIPTION = 160;

    /** El cuerpo se renderiza tal cual en el frontend: solo HTML inocuo. */
    private static final Safelist BODY_SAFELIST = new Safelist().addTags("p", "strong", "em");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
            Eres el redactor de noticias de DondeAnime, una web en español sobre anime y \
            dónde verlo en streaming en España e Hispanoamérica. Recibirás el título y el \
            extracto de una noticia en inglés. Redacta la versión en español siguiendo estas reglas:
            - Español neutro, tono informativo y directo, sin clickbait ni exclamaciones.
            - No inventes datos que no estén en el extracto. Si el extracto es muy corto, \
            el cuerpo también debe serlo.
            - Mantén los títulos de las obras en su forma oficial (no los traduzcas).
            - "titulo": máximo 200 caracteres. "resumen": máximo 500 caracteres.
            - "cuerpo": 2 o 3 párrafos cortos en HTML, usando solo etiquetas <p>.
            - "meta_titulo": máximo 70 caracteres. "meta_descripcion": máximo 160 caracteres.""";

    private static final JsonNode OUTPUT_SCHEMA = MAPPER.readTree("""
            {
              "type": "object",
              "properties": {
                "titulo": {"type": "string", "description": "Titular en español, máximo 200 caracteres"},
                "resumen": {"type": "string", "description": "Resumen en español, máximo 500 caracteres"},
                "cuerpo": {"type": "string", "description": "2 o 3 párrafos cortos en HTML, solo etiquetas <p>"},
                "meta_titulo": {"type": "string", "description": "Meta title SEO, máximo 70 caracteres"},
                "meta_descripcion": {"type": "string", "description": "Meta description SEO, máximo 160 caracteres"}
              },
              "required": ["titulo", "resumen", "cuerpo", "meta_titulo", "meta_descripcion"],
              "additionalProperties": false
            }
            """);

    private final LlmClient llmClient;
    private final int maxOutputTokens;

    public LlmNewsProcessor(
            LlmClient llmClient,
            @Value("${llm.max-output-tokens:1500}") int maxOutputTokens) {
        this.llmClient = llmClient;
        this.maxOutputTokens = maxOutputTokens;
    }

    public boolean enabled() {
        return llmClient.enabled();
    }

    /**
     * Redacta el ítem en español y guarda los tokens consumidos. El slug NO se
     * regenera: ya es la URL pública creada en la ingesta. El título/extracto
     * originales se conservan en sus columnas original_*.
     */
    public boolean enrich(NewsItem item) {
        try {
            LlmCompletion completion = llmClient.complete(
                    new LlmRequest(SYSTEM_PROMPT, userPrompt(item), maxOutputTokens, OUTPUT_SCHEMA));
            apply(item, MAPPER.readValue(completion.text(), LlmNewsResult.class));
            item.setLlmTokensUsed(completion.totalTokens());
            return true;
        } catch (RuntimeException e) {
            log.warn("El LLM falló redactando la noticia '{}': {}", item.getSlug(), e.getMessage());
            return false;
        }
    }

    private static String userPrompt(NewsItem item) {
        String title = firstText(item.getOriginalTitle(), item.getTitle());
        String excerpt = firstText(item.getOriginalExcerpt(), item.getSummary());
        StringBuilder prompt = new StringBuilder("Título original: ").append(title);
        if (!excerpt.isEmpty()) {
            prompt.append("\n\nExtracto original: ").append(excerpt);
        }
        prompt.append("\n\nFuente: ").append(item.getSourceName());
        return prompt.toString();
    }

    private static void apply(NewsItem item, LlmNewsResult result) {
        item.setTitle(truncate(requireText(result.titulo(), "titulo"), MAX_TITLE));
        item.setSummary(truncate(requireText(result.resumen(), "resumen"), MAX_SUMMARY));
        item.setBody(cleanBody(requireText(result.cuerpo(), "cuerpo")));
        item.setMetaTitle(truncate(requireText(result.metaTitulo(), "meta_titulo"), MAX_META_TITLE));
        item.setMetaDescription(truncate(requireText(result.metaDescripcion(), "meta_descripcion"), MAX_META_DESCRIPTION));
    }

    private static String cleanBody(String html) {
        String cleaned = Jsoup.clean(html, BODY_SAFELIST).trim();
        if (cleaned.isEmpty()) {
            throw new IllegalStateException("cuerpo quedo vacio tras sanear el HTML");
        }
        return cleaned;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("El LLM no devolvio el campo " + fieldName);
        }
        return value.trim();
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    /** Trunca sin partir surrogate pairs (emoji): un par cortado a la mitad
     *  produce un String UTF-16 inválido que corrompe el JSON/HTML. */
    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        int cut = max;
        if (Character.isHighSurrogate(value.charAt(cut - 1))) {
            cut--;
        }
        return value.substring(0, cut);
    }
}
