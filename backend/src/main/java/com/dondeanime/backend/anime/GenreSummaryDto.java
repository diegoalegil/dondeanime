package com.dondeanime.backend.anime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Género agregado: name, slug y cuántos anime hay con él.
 */
@Schema(description = "Genero agregado con slug y numero de anime")
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
