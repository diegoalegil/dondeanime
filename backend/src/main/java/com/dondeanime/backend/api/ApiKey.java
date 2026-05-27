package com.dondeanime.backend.api;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "api_key")
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_key", nullable = false, unique = true, length = 96)
    private String key;

    @Column(name = "owner_email", nullable = false, length = 255)
    private String ownerEmail;

    @Column(nullable = false, length = 32)
    private String tier;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "monthly_quota", nullable = false)
    private long monthlyQuota;

    @Column(name = "monthly_usage", nullable = false)
    private long monthlyUsage;

    public Long getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public long getMonthlyQuota() {
        return monthlyQuota;
    }

    public void setMonthlyQuota(long monthlyQuota) {
        this.monthlyQuota = monthlyQuota;
    }

    public long getMonthlyUsage() {
        return monthlyUsage;
    }

    public void setMonthlyUsage(long monthlyUsage) {
        this.monthlyUsage = monthlyUsage;
    }
}
