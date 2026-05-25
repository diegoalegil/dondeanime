package com.dondeanime.backend.api;

import java.time.Instant;

public record ApiKeyDto(
        Long id,
        String key,
        String ownerEmail,
        String tier,
        Instant createdAt,
        Instant lastUsedAt,
        long monthlyQuota,
        long monthlyUsage
) {
    public static ApiKeyDto from(ApiKey apiKey) {
        return new ApiKeyDto(
                apiKey.getId(),
                apiKey.getKey(),
                apiKey.getOwnerEmail(),
                apiKey.getTier(),
                apiKey.getCreatedAt(),
                apiKey.getLastUsedAt(),
                apiKey.getMonthlyQuota(),
                apiKey.getMonthlyUsage());
    }
}
