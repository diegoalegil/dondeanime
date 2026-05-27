package com.dondeanime.backend.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class EmbeddingRebuildServiceTest {

    private static final String HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private final EmbeddingDocumentBuilder documentBuilder = mock(EmbeddingDocumentBuilder.class);
    private final EmbeddingContentHasher contentHasher = mock(EmbeddingContentHasher.class);
    private final EmbeddingStorageService storageService = mock(EmbeddingStorageService.class);
    private final FakeEmbeddingClient embeddingClient = new FakeEmbeddingClient("test-model");
    private final EmbeddingRebuildService service = new EmbeddingRebuildService(
            documentBuilder,
            new EmbeddingDocumentTextFormatter(),
            contentHasher,
            embeddingClient,
            storageService);

    @Test
    void rebuildEmbedsAndStoresChangedDocuments() {
        AnimeSearchDocument document = document("attack-on-titan");
        when(documentBuilder.buildDocumentRecords()).thenReturn(List.of(new EmbeddingDocumentRecord(1L, document)));
        when(contentHasher.hash(document)).thenReturn(HASH);
        when(storageService.findByAnimeIdAndModel(1L, "test-model")).thenReturn(Optional.empty());

        EmbeddingRebuildResponse response = service.rebuild();

        assertThat(response.documentsPrepared()).isEqualTo(1);
        assertThat(response.embeddingsUpdated()).isEqualTo(1);
        assertThat(response.embeddingsSkipped()).isZero();
        assertThat(response.model()).isEqualTo("test-model");
        verify(storageService).upsert(1L, "test-model", HASH, List.of(0.1, 0.2, 0.3));
    }

    @Test
    void rebuildSkipsCurrentDocumentsWithoutCallingClient() {
        AnimeSearchDocument document = document("attack-on-titan");
        when(documentBuilder.buildDocumentRecords()).thenReturn(List.of(new EmbeddingDocumentRecord(1L, document)));
        when(contentHasher.hash(document)).thenReturn(HASH);
        when(storageService.findByAnimeIdAndModel(1L, "test-model")).thenReturn(Optional.of(new StoredAnimeEmbedding(
                1L,
                "test-model",
                HASH,
                List.of(0.9),
                Instant.parse("2026-05-27T10:00:00Z"))));

        EmbeddingRebuildResponse response = service.rebuild();

        assertThat(response.embeddingsUpdated()).isZero();
        assertThat(response.embeddingsSkipped()).isEqualTo(1);
        assertThat(embeddingClient.calls()).isZero();
        verify(storageService, never()).upsert(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    private static AnimeSearchDocument document(String slug) {
        return new AnimeSearchDocument(
                slug,
                "Attack on Titan",
                "Humanidad contra titanes.",
                List.of("Action"),
                "SPRING",
                2013,
                85,
                List.of(new AnimeSearchDocument.CountryPlatforms("ES", List.of("Crunchyroll"))),
                "https://dondeanime.com/anime/" + slug);
    }

    private static final class FakeEmbeddingClient implements EmbeddingClient {

        private final String model;
        private int calls;

        private FakeEmbeddingClient(String model) {
            this.model = model;
        }

        @Override
        public String model() {
            return model;
        }

        @Override
        public EmbeddingVector embed(String input) {
            calls++;
            return new EmbeddingVector(model, List.of(0.1, 0.2, 0.3));
        }

        int calls() {
            return calls;
        }
    }
}
