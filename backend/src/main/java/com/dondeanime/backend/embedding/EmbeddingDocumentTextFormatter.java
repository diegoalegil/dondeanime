package com.dondeanime.backend.embedding;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class EmbeddingDocumentTextFormatter {

    public String format(AnimeSearchDocument document) {
        String availability = document.availability().stream()
                .map(country -> country.countryCode() + ": " + String.join(", ", country.platforms()))
                .collect(Collectors.joining(" | "));

        return """
                Titulo: %s
                Sinopsis: %s
                Generos: %s
                Temporada: %s %s
                Score: %s
                Disponibilidad: %s
                URL: %s
                """.formatted(
                document.title(),
                document.spanishSynopsis(),
                String.join(", ", document.genres()),
                valueOrEmpty(document.season()),
                document.seasonYear() == null ? "" : document.seasonYear(),
                document.averageScore() == null ? "" : document.averageScore(),
                availability,
                document.canonicalUrl()).trim();
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
