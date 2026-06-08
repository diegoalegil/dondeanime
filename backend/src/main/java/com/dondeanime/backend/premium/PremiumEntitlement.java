package com.dondeanime.backend.premium;

import java.time.Instant;

public record PremiumEntitlement(
        String email,
        String planTier,
        Instant expiresAt) {
}
