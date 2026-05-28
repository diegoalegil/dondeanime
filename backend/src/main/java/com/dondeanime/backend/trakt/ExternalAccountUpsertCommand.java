package com.dondeanime.backend.trakt;

import java.time.Instant;

public record ExternalAccountUpsertCommand(
        String provider,
        String externalUserId,
        String email,
        String accessTokenCiphertext,
        String refreshTokenCiphertext,
        String scopes,
        Instant tokenExpiresAt) {
}
