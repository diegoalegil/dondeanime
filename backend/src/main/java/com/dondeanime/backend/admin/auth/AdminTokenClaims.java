package com.dondeanime.backend.admin.auth;

import java.time.Instant;

/** Claims de un token admin ya verificado (firma, tipo y expiración). */
public record AdminTokenClaims(String jti, Instant expiresAt) {
}
