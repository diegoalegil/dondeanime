package com.dondeanime.backend.provider;

/**
 * Vista pública de un WatchProvider. Sin id interno, sin animeId,
 * sin updatedAt, sin tmdbProviderId.
 */
public record ProviderDto(
        String countryCode,
        String providerName,
        String providerType,
        String logoUrl
) {
    public static ProviderDto from(WatchProvider p) {
        return new ProviderDto(
                p.getCountryCode(),
                p.getProviderName(),
                p.getProviderType(),
                p.getLogoUrl()
        );
    }
}
