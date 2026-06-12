package com.dondeanime.backend.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.dondeanime.backend.llm.LlmClient;
import com.dondeanime.backend.llm.LlmCompletion;
import com.dondeanime.backend.llm.LlmRequest;

class LlmNewsProcessorTest {

    private final LlmClient llmClient = mock(LlmClient.class);
    private final LlmNewsProcessor processor = new LlmNewsProcessor(llmClient, 1500);

    @Test
    void enrichAppliesSpanishFieldsAndTokenCount() {
        when(llmClient.complete(any(LlmRequest.class))).thenReturn(new LlmCompletion("""
                {
                  "titulo": "Solo Leveling anuncia segunda temporada",
                  "resumen": "El anime vuelve en enero con un nuevo trailer.",
                  "cuerpo": "<p>El anime vuelve en enero.</p><p>Habra nuevo trailer.</p>",
                  "meta_titulo": "Solo Leveling: segunda temporada",
                  "meta_descripcion": "El anime de Solo Leveling vuelve en enero."
                }
                """, 800, 150));
        NewsItem item = draft();

        boolean ok = processor.enrich(item);

        assertThat(ok).isTrue();
        assertThat(item.getTitle()).isEqualTo("Solo Leveling anuncia segunda temporada");
        assertThat(item.getSummary()).isEqualTo("El anime vuelve en enero con un nuevo trailer.");
        assertThat(item.getBody()).contains("<p>El anime vuelve en enero.</p>");
        assertThat(item.getMetaTitle()).isEqualTo("Solo Leveling: segunda temporada");
        assertThat(item.getMetaDescription()).isEqualTo("El anime de Solo Leveling vuelve en enero.");
        assertThat(item.getLlmTokensUsed()).isEqualTo(950);
        // El slug es la URL pública creada en la ingesta: no se regenera.
        assertThat(item.getSlug()).isEqualTo("solo-leveling-season-2-announced");
    }

    @Test
    void promptCarriesOriginalTitleExcerptSourceAndSchema() {
        when(llmClient.complete(any(LlmRequest.class))).thenReturn(new LlmCompletion("""
                {"titulo":"t","resumen":"r","cuerpo":"<p>c</p>","meta_titulo":"mt","meta_descripcion":"md"}
                """, 10, 10));
        ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);

        processor.enrich(draft());

        org.mockito.Mockito.verify(llmClient).complete(captor.capture());
        LlmRequest request = captor.getValue();
        assertThat(request.system()).contains("DondeAnime");
        assertThat(request.user())
                .contains("Solo Leveling season 2 announced")
                .contains("The anime returns in January.")
                .contains("ANN");
        assertThat(request.maxOutputTokens()).isEqualTo(1500);
        assertThat(request.outputSchema()).isNotNull();
        assertThat(request.outputSchema().get("required").toString()).contains("meta_descripcion");
    }

    @Test
    void bodyHtmlIsSanitizedToWhitelistedTags() {
        when(llmClient.complete(any(LlmRequest.class))).thenReturn(new LlmCompletion("""
                {"titulo":"t","resumen":"r",
                 "cuerpo":"<p>Texto <strong>fuerte</strong><script>alert(1)</script></p><div onclick=x>otro</div>",
                 "meta_titulo":"mt","meta_descripcion":"md"}
                """, 10, 10));
        NewsItem item = draft();

        assertThat(processor.enrich(item)).isTrue();
        assertThat(item.getBody()).doesNotContain("script").doesNotContain("onclick").doesNotContain("<div");
        assertThat(item.getBody()).contains("<strong>fuerte</strong>");
    }

    @Test
    void longFieldsAreTruncatedToColumnLimits() {
        when(llmClient.complete(any(LlmRequest.class))).thenReturn(new LlmCompletion("""
                {"titulo":"%s","resumen":"r","cuerpo":"<p>c</p>","meta_titulo":"%s","meta_descripcion":"md"}
                """.formatted("x".repeat(300), "y".repeat(100)), 10, 10));
        NewsItem item = draft();

        assertThat(processor.enrich(item)).isTrue();
        assertThat(item.getTitle()).hasSize(200);
        assertThat(item.getMetaTitle()).hasSize(70);
    }

    @Test
    void truncationDoesNotSplitSurrogatePairs() {
        // 199 chars + emoji (2 chars UTF-16) = 201: el corte en 200 partiría el
        // par y dejaría un String UTF-16 inválido; debe cortar en 199.
        String titulo = "x".repeat(199) + "👋";
        when(llmClient.complete(any(LlmRequest.class))).thenReturn(new LlmCompletion("""
                {"titulo":"%s","resumen":"r","cuerpo":"<p>c</p>","meta_titulo":"mt","meta_descripcion":"md"}
                """.formatted(titulo), 10, 10));
        NewsItem item = draft();

        assertThat(processor.enrich(item)).isTrue();
        assertThat(item.getTitle()).hasSize(199);
        assertThat(Character.isHighSurrogate(item.getTitle().charAt(item.getTitle().length() - 1))).isFalse();
    }

    @Test
    void invalidJsonReturnsFalseAndLeavesItemUntouched() {
        when(llmClient.complete(any(LlmRequest.class)))
                .thenReturn(new LlmCompletion("esto no es json", 10, 10));
        NewsItem item = draft();

        assertThat(processor.enrich(item)).isFalse();
        assertThat(item.getSummary()).isNull();
        assertThat(item.getLlmTokensUsed()).isNull();
    }

    @Test
    void missingFieldReturnsFalse() {
        when(llmClient.complete(any(LlmRequest.class))).thenReturn(new LlmCompletion("""
                {"titulo":"t","resumen":" ","cuerpo":"<p>c</p>","meta_titulo":"mt","meta_descripcion":"md"}
                """, 10, 10));

        assertThat(processor.enrich(draft())).isFalse();
    }

    @Test
    void llmClientErrorReturnsFalse() {
        when(llmClient.complete(any(LlmRequest.class)))
                .thenThrow(new IllegalStateException("El proveedor LLM no devolvio contenido"));

        assertThat(processor.enrich(draft())).isFalse();
    }

    @Test
    void enabledDelegatesToClient() {
        when(llmClient.enabled()).thenReturn(false);
        assertThat(processor.enabled()).isFalse();
    }

    private static NewsItem draft() {
        NewsItem item = new NewsItem();
        item.setSlug("solo-leveling-season-2-announced");
        item.setTitle("Solo Leveling season 2 announced");
        item.setOriginalTitle("Solo Leveling season 2 announced");
        item.setOriginalExcerpt("The anime returns in January.");
        item.setSourceUrl("https://news.example/solo-leveling");
        item.setSourceName("ANN");
        item.setStatus(NewsStatus.DRAFT);
        return item;
    }
}
