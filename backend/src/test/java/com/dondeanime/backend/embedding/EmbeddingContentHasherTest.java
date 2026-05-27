package com.dondeanime.backend.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class EmbeddingContentHasherTest {

    @Test
    void hashIsDeterministicSha256() {
        AnimeSearchDocument document = document();
        EmbeddingContentHasher hasher = new EmbeddingContentHasher();

        String first = hasher.hash(document);
        String second = hasher.hash(document);

        assertThat(first)
                .isEqualTo(second)
                .matches("[a-f0-9]{64}");
    }

    private static AnimeSearchDocument document() {
        return new AnimeSearchDocument(
                "attack-on-titan",
                "Attack on Titan",
                "Humanidad contra titanes.",
                List.of("Action", "Drama"),
                "SPRING",
                2013,
                85,
                List.of(new AnimeSearchDocument.CountryPlatforms("ES", List.of("Crunchyroll"))),
                "https://dondeanime.com/anime/attack-on-titan");
    }
}
