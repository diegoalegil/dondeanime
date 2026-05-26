package com.dondeanime.backend.admin.auth;

import java.time.Instant;

public record AdminLoginResponse(
        String token,
        String tokenType,
        Instant expiresAt) {
}
