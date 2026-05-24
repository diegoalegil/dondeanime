package com.dondeanime.backend.affiliate;

import java.time.Instant;

public record AffiliateLinkDto(
        Long id,
        String providerSlug,
        String countryCode,
        String affiliateUrl,
        Integer clickCount,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static AffiliateLinkDto from(AffiliateLink link) {
        return new AffiliateLinkDto(
                link.getId(),
                link.getProviderSlug(),
                link.getCountryCode(),
                link.getAffiliateUrl(),
                link.getClickCount(),
                link.isActive(),
                link.getCreatedAt(),
                link.getUpdatedAt());
    }
}
