package com.dondeanime.backend.push;

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
        name = "mobile_push_device",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_mobile_push_device_token", columnNames = "device_token")
        },
        indexes = {
                @Index(name = "idx_mobile_push_device_country", columnList = "country_iso"),
                @Index(name = "idx_mobile_push_device_platform", columnList = "platform")
        })
public class MobilePushDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "platform", nullable = false, length = 16)
    private String platform;

    @Column(name = "device_token", nullable = false, length = 512)
    private String deviceToken;

    @Column(name = "country_iso", nullable = false, length = 2)
    private String countryIso;

    @Column(name = "alerts_only", nullable = false)
    private Boolean alertsOnly;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public MobilePushDevice() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getCountryIso() {
        return countryIso;
    }

    public void setCountryIso(String countryIso) {
        this.countryIso = countryIso;
    }

    public Boolean getAlertsOnly() {
        return alertsOnly;
    }

    public void setAlertsOnly(Boolean alertsOnly) {
        this.alertsOnly = alertsOnly;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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
