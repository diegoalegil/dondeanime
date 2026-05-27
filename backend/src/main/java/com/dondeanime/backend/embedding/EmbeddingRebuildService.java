package com.dondeanime.backend.embedding;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class EmbeddingRebuildService {

    private final EmbeddingDocumentBuilder documentBuilder;
    private final EmbeddingDocumentTextFormatter textFormatter;
    private final EmbeddingContentHasher contentHasher;
    private final EmbeddingClient embeddingClient;
    private final EmbeddingStorageService storageService;

    public EmbeddingRebuildService(
            EmbeddingDocumentBuilder documentBuilder,
            EmbeddingDocumentTextFormatter textFormatter,
            EmbeddingContentHasher contentHasher,
            EmbeddingClient embeddingClient,
            EmbeddingStorageService storageService) {
        this.documentBuilder = documentBuilder;
        this.textFormatter = textFormatter;
        this.contentHasher = contentHasher;
        this.embeddingClient = embeddingClient;
        this.storageService = storageService;
    }

    public EmbeddingRebuildResponse rebuild() {
        List<EmbeddingDocumentRecord> records = documentBuilder.buildDocumentRecords();
        int updated = 0;
        int skipped = 0;

        for (EmbeddingDocumentRecord record : records) {
            String contentHash = contentHasher.hash(record.document());
            if (isCurrent(record.animeId(), embeddingClient.model(), contentHash)) {
                skipped++;
                continue;
            }

            EmbeddingVector vector = embeddingClient.embed(textFormatter.format(record.document()));
            storageService.upsert(record.animeId(), vector.model(), contentHash, vector.values());
            updated++;
        }

        return new EmbeddingRebuildResponse(records.size(), updated, skipped, embeddingClient.model());
    }

    private boolean isCurrent(Long animeId, String model, String contentHash) {
        return storageService.findByAnimeIdAndModel(animeId, model)
                .map(stored -> stored.contentHash().equals(contentHash))
                .orElse(false);
    }
}
