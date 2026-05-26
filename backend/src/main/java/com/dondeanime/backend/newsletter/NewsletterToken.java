package com.dondeanime.backend.newsletter;

import java.time.Instant;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "newsletter_token",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_newsletter_token_hash",
                columnNames = "token_hash"),
        indexes = {
                @Index(name = "idx_newsletter_token_subscriber", columnList = "subscriber_id"),
                @Index(name = "idx_newsletter_token_expires", columnList = "expires_at")
        })
public class NewsletterToken {

    public static final String TYPE_CONFIRMATION = "NEWSLETTER_CONFIRMATION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "subscriber_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_newsletter_token_subscriber"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private NewsletterSubscriber subscriber;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "token_type", nullable = false, length = 40)
    private String tokenType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    public NewsletterToken() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NewsletterSubscriber getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(NewsletterSubscriber subscriber) {
        this.subscriber = subscriber;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }
}
