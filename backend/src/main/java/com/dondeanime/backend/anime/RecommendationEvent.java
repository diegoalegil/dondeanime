package com.dondeanime.backend.anime;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "recommendation_event",
        indexes = {
                @Index(name = "idx_recommendation_event_clicked_at", columnList = "clicked_at"),
                @Index(name = "idx_recommendation_event_source", columnList = "source_anime_slug"),
                @Index(name = "idx_recommendation_event_target", columnList = "target_anime_slug")
        })
public class RecommendationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_anime_slug", nullable = false, length = 180)
    private String sourceAnimeSlug;

    @Column(name = "target_anime_slug", nullable = false, length = 180)
    private String targetAnimeSlug;

    @Column(name = "clicked_at", nullable = false)
    private Instant clickedAt;

    public RecommendationEvent() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSourceAnimeSlug() {
        return sourceAnimeSlug;
    }

    public void setSourceAnimeSlug(String sourceAnimeSlug) {
        this.sourceAnimeSlug = sourceAnimeSlug;
    }

    public String getTargetAnimeSlug() {
        return targetAnimeSlug;
    }

    public void setTargetAnimeSlug(String targetAnimeSlug) {
        this.targetAnimeSlug = targetAnimeSlug;
    }

    public Instant getClickedAt() {
        return clickedAt;
    }

    public void setClickedAt(Instant clickedAt) {
        this.clickedAt = clickedAt;
    }
}
