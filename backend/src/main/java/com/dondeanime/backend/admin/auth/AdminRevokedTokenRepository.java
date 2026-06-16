package com.dondeanime.backend.admin.auth;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminRevokedTokenRepository extends JpaRepository<AdminRevokedToken, String> {

    long deleteByExpiresAtBefore(Instant cutoff);

    /**
     * Inserta el jti revocado de forma idempotente y sin carrera: dos logout
     * concurrentes del mismo token no chocan en la PK (antes el patrón
     * existsById+save dejaba una ventana TOCTOU que provocaba
     * DataIntegrityViolationException -> 500). {@code ON CONFLICT DO NOTHING}
     * lo resuelve atómicamente en la BD.
     */
    @Modifying
    @Query(value = "INSERT INTO admin_revoked_token (jti, expires_at, revoked_at) "
            + "VALUES (:jti, :expiresAt, :revokedAt) ON CONFLICT (jti) DO NOTHING", nativeQuery = true)
    void insertIfAbsent(@Param("jti") String jti,
                        @Param("expiresAt") Instant expiresAt,
                        @Param("revokedAt") Instant revokedAt);
}
