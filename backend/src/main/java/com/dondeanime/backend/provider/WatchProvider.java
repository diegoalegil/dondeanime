package com.dondeanime.backend.provider;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Plataforma de streaming donde se puede ver un anime en un país concreto.
 *
 * No usamos @ManyToOne con Anime: guardamos solo animeId (Long) para
 * evitar lazy-loading y mantener el modelo simple. El service que une
 * Anime + sus WatchProviders se encarga de la composición.
 */
@Entity
@Table(
        name = "watch_provider",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_watch_provider_anime_country_name",
                columnNames = {"anime_id", "country_code", "provider_name"}
        ),
        indexes = {
                @Index(name = "idx_watch_provider_anime", columnList = "anime_id"),
                @Index(name = "idx_watch_provider_country", columnList = "country_code")
        }
)
public class WatchProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anime_id", nullable = false)
    private Long animeId;

    /** Código ISO 3166-1 alpha-2 (ES, MX, AR, CO, CL, ...). */
    @Column(name = "country_code", length = 2, nullable = false)
    private String countryCode;

    @Column(name = "provider_name", length = 100, nullable = false)
    private String providerName;

    /** FLATRATE (incluido en suscripción), FREE, RENT, BUY. */
    @Column(name = "provider_type", length = 20, nullable = false)
    private String providerType;

    /** ID de TMDb para el provider (Crunchyroll=283, Netflix=8, ...). Cache. */
    @Column(name = "tmdb_provider_id")
    private Integer tmdbProviderId;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public WatchProvider() {
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

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public Integer getTmdbProviderId() {
        return tmdbProviderId;
    }

    public void setTmdbProviderId(Integer tmdbProviderId) {
        this.tmdbProviderId = tmdbProviderId;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
