package com.dondeanime.backend.studio;

public record StudioDto(
        String slug,
        String name,
        boolean animationStudio
) {
    public static StudioDto from(Studio studio) {
        return new StudioDto(studio.getSlug(), studio.getName(), studio.isAnimationStudio());
    }
}
