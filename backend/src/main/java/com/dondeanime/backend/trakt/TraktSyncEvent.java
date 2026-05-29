package com.dondeanime.backend.trakt;

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
        name = "trakt_sync_event",
        indexes = {
                @Index(name = "idx_trakt_sync_event_provider", columnList = "provider"),
                @Index(name = "idx_trakt_sync_event_synced_at", columnList = "synced_at")
        })
public class TraktSyncEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    @Column(name = "watched_imported", nullable = false)
    private Integer watchedImported;

    @Column(name = "ratings_imported", nullable = false)
    private Integer ratingsImported;

    @Column(name = "unmatched_count", nullable = false)
    private Integer unmatchedCount;

    public TraktSyncEvent() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(Instant syncedAt) {
        this.syncedAt = syncedAt;
    }

    public Integer getWatchedImported() {
        return watchedImported;
    }

    public void setWatchedImported(Integer watchedImported) {
        this.watchedImported = watchedImported;
    }

    public Integer getRatingsImported() {
        return ratingsImported;
    }

    public void setRatingsImported(Integer ratingsImported) {
        this.ratingsImported = ratingsImported;
    }

    public Integer getUnmatchedCount() {
        return unmatchedCount;
    }

    public void setUnmatchedCount(Integer unmatchedCount) {
        this.unmatchedCount = unmatchedCount;
    }
}
