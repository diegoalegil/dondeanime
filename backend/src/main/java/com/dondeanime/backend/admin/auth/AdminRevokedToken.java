package com.dondeanime.backend.admin.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** jti de sesión admin revocada; se conserva hasta que el token expire. */
@Entity
@Table(name = "admin_revoked_token")
public class AdminRevokedToken {

    @Id
    @Column(length = 36)
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;

    public AdminRevokedToken() {
    }

    public AdminRevokedToken(String jti, Instant expiresAt, Instant revokedAt) {
        this.jti = jti;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }

    public String getJti() {
        return jti;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
