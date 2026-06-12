package com.dondeanime.backend.admin.auth;

import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Denylist de sesiones admin. El JWT es stateless (TTL 8h): sin esto, un
 * token filtrado vale hasta que expire. Revocar anota el jti hasta su exp;
 * el filtro consulta la denylist en cada petición admin (tráfico mínimo y
 * ya rate-limitado, una query indexada por PK no pesa).
 */
@Service
public class AdminTokenRevocationService {

    private final AdminRevokedTokenRepository repository;
    private final Clock clock;

    @Autowired
    public AdminTokenRevocationService(AdminRevokedTokenRepository repository) {
        this(repository, Clock.systemUTC());
    }

    AdminTokenRevocationService(AdminRevokedTokenRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Idempotente. De paso purga entradas expiradas (sin cron propio). */
    @Transactional
    public void revoke(AdminTokenClaims claims) {
        Instant now = clock.instant();
        repository.deleteByExpiresAtBefore(now);
        if (!repository.existsById(claims.jti())) {
            repository.save(new AdminRevokedToken(claims.jti(), claims.expiresAt(), now));
        }
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String jti) {
        return repository.existsById(jti);
    }
}
