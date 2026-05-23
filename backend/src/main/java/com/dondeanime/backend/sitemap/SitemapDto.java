package com.dondeanime.backend.sitemap;

import java.util.List;

/**
 * Lista plana de identificadores que el frontend usa para generar
 * sitemap.xml. El backend NO conoce las URLs del frontend; solo
 * expone los datos. El frontend mapea cada tipo a su pattern de URL.
 *
 * Ejemplo de mapeo en el frontend:
 *   animeSlugs  → /anime/{slug}
 *   providers   → /donde-ver/{slug}/{country}
 *   genreSlugs  → /genero/{slug}
 *   seasons     → /temporada/{year}/{season}
 */
public record SitemapDto(
        List<String> animeSlugs,
        List<ProviderCountryEntry> providers,
        List<String> genreSlugs,
        List<SeasonEntry> seasons
) {
    public record ProviderCountryEntry(String slug, String country) {}
    public record SeasonEntry(int year, String season) {}
}
