package com.dondeanime.backend.admin.auth;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRevokedTokenRepository extends JpaRepository<AdminRevokedToken, String> {

    long deleteByExpiresAtBefore(Instant cutoff);
}
