package com.dondeanime.backend.curated;

import java.time.Instant;

public record CuratorProfileDto(
        String email,
        String displayName,
        boolean approved,
        Instant approvedAt,
        Instant revokedAt
) {
    public static CuratorProfileDto from(CuratorProfile profile) {
        return new CuratorProfileDto(
                profile.getEmail(),
                profile.getDisplayName(),
                profile.isApproved(),
                profile.getApprovedAt(),
                profile.getRevokedAt());
    }
}
