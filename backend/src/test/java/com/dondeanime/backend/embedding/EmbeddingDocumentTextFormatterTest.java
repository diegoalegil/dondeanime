package com.dondeanime.backend.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class EmbeddingDocumentTextFormatterTest {

    @Test
    void formatsDocumentForEmbeddingInput() {
        AnimeSearchDocument document = new AnimeSearchDocument(
                "attack-on-titan",
                "Attack on Titan",
                "Humanidad contra titanes.",
                List.of("Action", "Drama"),
                "SPRING",
                2013,
                85,
                List.of(new AnimeSearchDocument.CountryPlatforms("ES", List.of("Crunchyroll", "Netflix"))),
                "https://dondeanime.com/anime/attack-on-titan");

        String text = new EmbeddingDocumentTextFormatter().format(document);

        assertThat(text)
                .contains("Titulo: Attack on Titan")
                .contains("Generos: Action, Drama")
                .contains("Disponibilidad: ES: Crunchyroll, Netflix")
                .contains("URL: https://dondeanime.com/anime/attack-on-titan");
    }
}
