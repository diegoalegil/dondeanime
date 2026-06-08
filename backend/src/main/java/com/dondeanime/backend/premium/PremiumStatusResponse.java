package com.dondeanime.backend.premium;

import java.time.Instant;

public record PremiumStatusResponse(
        boolean premium,
        String planTier,
        Instant expiresAt) {

    static PremiumStatusResponse active(PremiumEntitlement entitlement) {
        return new PremiumStatusResponse(true, entitlement.planTier(), entitlement.expiresAt());
    }

    static PremiumStatusResponse inactive() {
        return new PremiumStatusResponse(false, null, null);
    }
}
