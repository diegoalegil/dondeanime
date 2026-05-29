package com.dondeanime.backend.embedding;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class EmbeddingStorageService {

    private final AnimeEmbeddingRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public EmbeddingStorageService(AnimeEmbeddingRepository repository, Clock clock) {
        this(repository, new ObjectMapper(), clock);
    }

    EmbeddingStorageService(
            AnimeEmbeddingRepository repository,
            ObjectMapper objectMapper,
            Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public AnimeEmbedding upsert(Long animeId, String model, String contentHash, List<Double> embedding) {
        Long safeAnimeId = requireAnimeId(animeId);
        String safeModel = normalizeRequired(model, "model");
        String safeContentHash = normalizeContentHash(contentHash);
        List<Double> safeEmbedding = normalizeEmbedding(embedding);

        AnimeEmbedding entity = repository.findByAnimeIdAndModel(safeAnimeId, safeModel)
                .orElseGet(AnimeEmbedding::new);
        entity.setAnimeId(safeAnimeId);
        entity.setModel(safeModel);
        entity.setContentHash(safeContentHash);
        entity.setEmbedding(serialize(safeEmbedding));
        entity.setUpdatedAt(clock.instant());
        return repository.save(entity);
    }

    public List<StoredAnimeEmbedding> findByModel(String model) {
        String safeModel = normalizeRequired(model, "model");
        return repository.findByModelOrderByUpdatedAtDesc(safeModel).stream()
                .map(this::toStoredEmbedding)
                .toList();
    }

    public Optional<StoredAnimeEmbedding> findByAnimeIdAndModel(Long animeId, String model) {
        Long safeAnimeId = requireAnimeId(animeId);
        String safeModel = normalizeRequired(model, "model");
        return repository.findByAnimeIdAndModel(safeAnimeId, safeModel)
                .map(this::toStoredEmbedding);
    }

    private StoredAnimeEmbedding toStoredEmbedding(AnimeEmbedding entity) {
        return new StoredAnimeEmbedding(
                entity.getAnimeId(),
                entity.getModel(),
                entity.getContentHash(),
                deserialize(entity.getEmbedding()),
                entity.getUpdatedAt());
    }

    private String serialize(List<Double> embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("No se pudo serializar embedding", e);
        }
    }

    private List<Double> deserialize(String embeddingJson) {
        try {
            double[] values = objectMapper.readValue(embeddingJson, double[].class);
            return Arrays.stream(values).boxed().toList();
        } catch (JacksonException e) {
            throw new IllegalStateException("Embedding almacenado invalido", e);
        }
    }

    private static Long requireAnimeId(Long animeId) {
        if (animeId == null || animeId < 1) {
            throw new IllegalArgumentException("animeId debe ser positivo");
        }
        return animeId;
    }

    private static String normalizeContentHash(String contentHash) {
        String normalized = normalizeRequired(contentHash, "contentHash").toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("contentHash debe ser SHA-256 en hexadecimal");
        }
        return normalized;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " no puede estar vacio");
        }
        return value.trim();
    }

    private static List<Double> normalizeEmbedding(List<Double> embedding) {
        if (embedding == null || embedding.isEmpty()) {
            throw new IllegalArgumentException("embedding no puede estar vacio");
        }
        List<Double> normalized = embedding.stream()
                .filter(Objects::nonNull)
                .peek(value -> {
                    if (!Double.isFinite(value)) {
                        throw new IllegalArgumentException("embedding contiene valores no finitos");
                    }
                })
                .toList();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("embedding no puede estar vacio");
        }
        return normalized;
    }
}
