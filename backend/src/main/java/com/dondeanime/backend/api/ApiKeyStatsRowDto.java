package com.dondeanime.backend.api;

import java.time.Instant;

public record ApiKeyStatsRowDto(
        Long id,
        String keyPreview,
        String ownerEmail,
        String tier,
        Instant createdAt,
        Instant lastUsedAt,
        long monthlyQuota,
        long monthlyUsage,
        long remaining) {

    static ApiKeyStatsRowDto from(ApiKey apiKey) {
        long remaining = Math.max(0, apiKey.getMonthlyQuota() - apiKey.getMonthlyUsage());
        return new ApiKeyStatsRowDto(
                apiKey.getId(),
                preview(apiKey.getKey()),
                apiKey.getOwnerEmail(),
                apiKey.getTier(),
                apiKey.getCreatedAt(),
                apiKey.getLastUsedAt(),
                apiKey.getMonthlyQuota(),
                apiKey.getMonthlyUsage(),
                remaining);
    }

    private static String preview(String key) {
        if (key == null || key.length() <= 16) {
            return key;
        }
        return key.substring(0, 12) + "..." + key.substring(key.length() - 4);
    }
}
