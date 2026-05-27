package com.dondeanime.backend.trakt;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "user_watched_anime",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_watched_anime_account_slug_source",
                        columnNames = {"external_account_id", "anime_slug", "source"})
        })
public class UserWatchedAnime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "external_account_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_watched_anime_external_account"))
    private ExternalAccount externalAccount;

    @Column(name = "anime_slug", nullable = false, length = 255)
    private String animeSlug;

    @Column(name = "watched_at", nullable = false)
    private Instant watchedAt;

    @Column(name = "source", nullable = false, length = 32)
    private String source;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UserWatchedAnime() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExternalAccount getExternalAccount() {
        return externalAccount;
    }

    public void setExternalAccount(ExternalAccount externalAccount) {
        this.externalAccount = externalAccount;
    }

    public String getAnimeSlug() {
        return animeSlug;
    }

    public void setAnimeSlug(String animeSlug) {
        this.animeSlug = animeSlug;
    }

    public Instant getWatchedAt() {
        return watchedAt;
    }

    public void setWatchedAt(Instant watchedAt) {
        this.watchedAt = watchedAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
