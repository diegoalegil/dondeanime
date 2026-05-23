package com.dondeanime.backend.anime;

/**
 * Género agregado: name, slug y cuántos anime hay con él.
 */
public record GenreSummaryDto(
        String slug,
        String name,
        long animeCount
) {
    /** "Slice of Life" → "slice-of-life". */
    public static String slugify(String name) {
        return name.toLowerCase().replace(' ', '-');
    }

    public static GenreSummaryDto of(String name, long animeCount) {
        return new GenreSummaryDto(slugify(name), name, animeCount);
    }
}
