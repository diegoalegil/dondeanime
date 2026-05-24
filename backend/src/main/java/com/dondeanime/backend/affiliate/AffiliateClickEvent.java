package com.dondeanime.backend.affiliate;

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
        name = "affiliate_click_event",
        indexes = {
                @Index(name = "idx_affiliate_click_clicked_at", columnList = "clicked_at"),
                @Index(name = "idx_affiliate_click_anime", columnList = "anime_slug"),
                @Index(name = "idx_affiliate_click_provider_country", columnList = "provider_slug,country_code")
        })
public class AffiliateClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "affiliate_link_id", nullable = false)
    private Long affiliateLinkId;

    @Column(name = "provider_slug", nullable = false, length = 100)
    private String providerSlug;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "anime_slug", nullable = false, length = 180)
    private String animeSlug;

    @Column(name = "clicked_at", nullable = false)
    private Instant clickedAt;

    public AffiliateClickEvent() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAffiliateLinkId() {
        return affiliateLinkId;
    }

    public void setAffiliateLinkId(Long affiliateLinkId) {
        this.affiliateLinkId = affiliateLinkId;
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

    public String getAnimeSlug() {
        return animeSlug;
    }

    public void setAnimeSlug(String animeSlug) {
        this.animeSlug = animeSlug;
    }

    public Instant getClickedAt() {
        return clickedAt;
    }

    public void setClickedAt(Instant clickedAt) {
        this.clickedAt = clickedAt;
    }
}
