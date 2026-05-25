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
        name = "push_subscription",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_push_subscription_endpoint",
                columnNames = "endpoint"),
        indexes = {
                @Index(name = "idx_push_subscription_country", columnList = "country_iso"),
                @Index(name = "idx_push_subscription_email", columnList = "user_email")
        })
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;

    @Column(name = "endpoint", nullable = false, columnDefinition = "TEXT")
    private String endpoint;

    @Column(name = "p256dh", nullable = false, columnDefinition = "TEXT")
    private String p256dh;

    @Column(name = "auth", nullable = false, columnDefinition = "TEXT")
    private String auth;

    @Column(name = "country_iso", nullable = false, length = 2)
    private String countryIso;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "delivery_success_count")
    private Integer deliverySuccessCount;

    @Column(name = "delivery_failure_count")
    private Integer deliveryFailureCount;

    @Column(name = "last_status_code")
    private Integer lastStatusCode;

    @Column(name = "last_delivered_at")
    private Instant lastDeliveredAt;

    @Column(name = "last_failed_at")
    private Instant lastFailedAt;

    public PushSubscription() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getP256dh() {
        return p256dh;
    }

    public void setP256dh(String p256dh) {
        this.p256dh = p256dh;
    }

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }

    public String getCountryIso() {
        return countryIso;
    }

    public void setCountryIso(String countryIso) {
        this.countryIso = countryIso;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getDeliverySuccessCount() {
        return deliverySuccessCount;
    }

    public void setDeliverySuccessCount(Integer deliverySuccessCount) {
        this.deliverySuccessCount = deliverySuccessCount;
    }

    public Integer getDeliveryFailureCount() {
        return deliveryFailureCount;
    }

    public void setDeliveryFailureCount(Integer deliveryFailureCount) {
        this.deliveryFailureCount = deliveryFailureCount;
    }

    public Integer getLastStatusCode() {
        return lastStatusCode;
    }

    public void setLastStatusCode(Integer lastStatusCode) {
        this.lastStatusCode = lastStatusCode;
    }

    public Instant getLastDeliveredAt() {
        return lastDeliveredAt;
    }

    public void setLastDeliveredAt(Instant lastDeliveredAt) {
        this.lastDeliveredAt = lastDeliveredAt;
    }

    public Instant getLastFailedAt() {
        return lastFailedAt;
    }

    public void setLastFailedAt(Instant lastFailedAt) {
        this.lastFailedAt = lastFailedAt;
    }

    public void recordDeliveryResult(int statusCode, Instant at) {
        this.lastStatusCode = statusCode;
        if (statusCode >= 200 && statusCode < 300) {
            this.deliverySuccessCount = count(deliverySuccessCount) + 1;
            this.lastDeliveredAt = at;
        } else {
            this.deliveryFailureCount = count(deliveryFailureCount) + 1;
            this.lastFailedAt = at;
        }
    }

    private static int count(Integer value) {
        return value == null ? 0 : value;
    }
}
