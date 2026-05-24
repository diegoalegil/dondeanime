package com.dondeanime.backend.affiliate;

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
        name = "affiliate_link",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_affiliate_link_provider_country",
                columnNames = {"provider_slug", "country_code"}),
        indexes = {
                @Index(name = "idx_affiliate_link_provider_country", columnList = "provider_slug,country_code"),
                @Index(name = "idx_affiliate_link_active", columnList = "active")
        })
public class AffiliateLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_slug", nullable = false, length = 100)
    private String providerSlug;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "affiliate_url", nullable = false, columnDefinition = "TEXT")
    private String affiliateUrl;

    @Column(name = "click_count", nullable = false)
    private Integer clickCount = 0;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AffiliateLink() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProviderSlug() {
        return providerSlug;
    }

    public void setProviderSlug(String providerSlug) {
        this.providerSlug = providerSlug;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getAffiliateUrl() {
        return affiliateUrl;
    }

    public void setAffiliateUrl(String affiliateUrl) {
        this.affiliateUrl = affiliateUrl;
    }

    public Integer getClickCount() {
        return clickCount;
    }

    public void setClickCount(Integer clickCount) {
        this.clickCount = clickCount;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
