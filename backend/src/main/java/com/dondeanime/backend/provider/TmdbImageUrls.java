package com.dondeanime.backend.provider;

/**
 * Compone la URL absoluta del logo de un proveedor a partir del {@code logo_path}
 * relativo que devuelve TMDb. Es POLÍTICA de presentación de DondeAnime: la
 * librería Tsunagi devuelve el path crudo, y aquí elegimos el tamaño/CDN.
 *
 * <p>Antes vivía como método estático en el {@code TmdbClient} propio (ya borrado).
 */
public final class TmdbImageUrls {

    private static final String IMAGE_BASE = "https://image.tmdb.org/t/p/original";

    private TmdbImageUrls() {
    }

    /** Convierte un {@code logo_path} relativo de TMDb en URL absoluta, o null si está vacío. */
    public static String fullLogoUrl(String logoPath) {
        if (logoPath == null || logoPath.isBlank()) {
            return null;
        }
        return IMAGE_BASE + logoPath;
    }
}
