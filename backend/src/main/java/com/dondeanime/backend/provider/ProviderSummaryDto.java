package com.dondeanime.backend.provider;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Plataforma de streaming agregada a nivel global (sin país).
 * El frontend la usa para listar todas las plataformas que cubrimos
 * y mostrar páginas tipo "/donde-ver/crunchyroll".
 *
 * slug se calcula desde providerName: lowercase + espacios→guiones.
 * animeCount es el número de anime distintos que están en este provider
 * (sumando todos los países).
 */
@Schema(description = "Plataforma de streaming agregada")
public record ProviderSummaryDto(
        String slug,
        String providerName,
        String logoUrl,
        long animeCount
) {
    /** Convención de slugificación para provider names ("Amazon Prime Video" → "amazon-prime-video"). */
    public static String slugify(String providerName) {
        return providerName.toLowerCase().replace(' ', '-');
    }

    public static ProviderSummaryDto of(String providerName, String logoUrl, long animeCount) {
        return new ProviderSummaryDto(slugify(providerName), providerName, logoUrl, animeCount);
    }
}
