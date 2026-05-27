package com.dondeanime.backend.studio;

/**
 * Estudio agregado para listados publicos.
 */
public record StudioSummaryDto(
        String slug,
        String name,
        boolean animationStudio,
        long animeCount
) {
    public static StudioSummaryDto from(Studio studio, long animeCount) {
        return new StudioSummaryDto(
                studio.getSlug(),
                studio.getName(),
                studio.isAnimationStudio(),
                animeCount);
    }
}
