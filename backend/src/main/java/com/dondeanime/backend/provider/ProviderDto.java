package com.dondeanime.backend.provider;

/**
 * Vista pública de un WatchProvider. Sin id interno, sin animeId,
 * sin updatedAt, sin tmdbProviderId.
 *
 * Incluye providerSlug (normalizado desde providerName) para que el
 * frontend pueda enlazar a /plataforma/{slug} sin duplicar la regla
 * de slugificación.
 */
public record ProviderDto(
        String countryCode,
        String providerSlug,
        String providerName,
        String providerType,
        String logoUrl,
        String affiliateUrl
) {
    public static ProviderDto from(WatchProvider p) {
        return from(p, null);
    }

    public static ProviderDto from(WatchProvider p, String affiliateUrl) {
        return new ProviderDto(
                p.getCountryCode(),
                ProviderSummaryDto.slugify(p.getProviderName()),
                p.getProviderName(),
                p.getProviderType(),
                p.getLogoUrl(),
                affiliateUrl
        );
    }
}
