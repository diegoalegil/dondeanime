package com.dondeanime.backend.admin.auth;

public record AdminTotpSetupResponse(
        String secret,
        String qrCodeData,
        String otpauthUri,
        boolean enabled) {
}
