package com.dondeanime.backend.embedding;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "anime_embedding",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_anime_embedding_anime_model",
                columnNames = {"anime_id", "model"}),
        indexes = {
                @Index(name = "idx_anime_embedding_model", columnList = "model"),
                @Index(name = "idx_anime_embedding_content_hash", columnList = "content_hash")
        })
public class AnimeEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anime_id", nullable = false)
    private Long animeId;

    @Column(name = "model", nullable = false, length = 120)
    private String model;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "embedding", nullable = false, columnDefinition = "TEXT")
    private String embedding;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AnimeEmbedding() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAnimeId() {
        return animeId;
    }

    public void setAnimeId(Long animeId) {
        this.animeId = animeId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getEmbedding() {
        return embedding;
    }

    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
