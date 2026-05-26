package com.dondeanime.backend.api;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "api_key_endpoint_usage",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_api_key_endpoint_usage_key_endpoint",
                columnNames = {"api_key_id", "endpoint"}))
public class ApiKeyEndpointUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "api_key_id", nullable = false)
    private ApiKey apiKey;

    @Column(nullable = false, length = 255)
    private String endpoint;

    @Column(name = "monthly_usage", nullable = false)
    private long monthlyUsage;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    public Long getId() {
        return id;
    }

    public ApiKey getApiKey() {
        return apiKey;
    }

    public void setApiKey(ApiKey apiKey) {
        this.apiKey = apiKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public long getMonthlyUsage() {
        return monthlyUsage;
    }

    public void setMonthlyUsage(long monthlyUsage) {
        this.monthlyUsage = monthlyUsage;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
