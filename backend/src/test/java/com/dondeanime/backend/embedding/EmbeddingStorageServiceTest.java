package com.dondeanime.backend.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import tools.jackson.databind.ObjectMapper;

class EmbeddingStorageServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-27T08:00:00Z");
    private static final String HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private final AnimeEmbeddingRepository repository = mock(AnimeEmbeddingRepository.class);
    private final EmbeddingStorageService service = new EmbeddingStorageService(
            repository,
            new ObjectMapper(),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void upsertCreatesSerializedEmbedding() {
        when(repository.findByAnimeIdAndModel(1L, "test-embedding-model")).thenReturn(Optional.empty());

        service.upsert(1L, " test-embedding-model ", HASH.toUpperCase(), List.of(0.1, -0.2, 0.3));

        ArgumentCaptor<AnimeEmbedding> captor = ArgumentCaptor.forClass(AnimeEmbedding.class);
        verify(repository).save(captor.capture());
        AnimeEmbedding saved = captor.getValue();
        assertThat(saved.getAnimeId()).isEqualTo(1L);
        assertThat(saved.getModel()).isEqualTo("test-embedding-model");
        assertThat(saved.getContentHash()).isEqualTo(HASH);
        assertThat(saved.getEmbedding()).isEqualTo("[0.1,-0.2,0.3]");
        assertThat(saved.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void upsertUpdatesExistingRow() {
        AnimeEmbedding existing = new AnimeEmbedding();
        existing.setId(10L);
        existing.setAnimeId(1L);
        existing.setModel("old");
        existing.setContentHash(HASH);
        existing.setEmbedding("[0.0]");
        existing.setUpdatedAt(NOW.minusSeconds(60));
        when(repository.findByAnimeIdAndModel(1L, "test-embedding-model")).thenReturn(Optional.of(existing));

        service.upsert(1L, "test-embedding-model", HASH, List.of(0.4));

        assertThat(existing.getId()).isEqualTo(10L);
        assertThat(existing.getModel()).isEqualTo("test-embedding-model");
        assertThat(existing.getEmbedding()).isEqualTo("[0.4]");
        assertThat(existing.getUpdatedAt()).isEqualTo(NOW);
        verify(repository).save(existing);
    }

    @Test
    void findByModelDeserializesStoredVectors() {
        AnimeEmbedding stored = new AnimeEmbedding();
        stored.setAnimeId(7L);
        stored.setModel("test-embedding-model");
        stored.setContentHash(HASH);
        stored.setEmbedding("[0.5,-0.25]");
        stored.setUpdatedAt(NOW);
        when(repository.findByModelOrderByUpdatedAtDesc("test-embedding-model")).thenReturn(List.of(stored));

        List<StoredAnimeEmbedding> result = service.findByModel(" test-embedding-model ");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().animeId()).isEqualTo(7L);
        assertThat(result.getFirst().embedding()).containsExactly(0.5, -0.25);
    }

    @Test
    void upsertRejectsInvalidVector() {
        assertThatThrownBy(() -> service.upsert(1L, "model", HASH, List.of(Double.NaN)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no finitos");
    }

    @Test
    void upsertRejectsInvalidHash() {
        assertThatThrownBy(() -> service.upsert(1L, "model", "abc", List.of(0.1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SHA-256");
    }
}
