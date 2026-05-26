package com.dondeanime.backend.anime;

import java.util.List;

public record BeginnerAnimeDto(
        AnimeSummaryDto anime,
        String beginnerRecommendation
) {
    public static BeginnerAnimeDto from(Anime anime, List<AnimeOverride> overrides) {
        String recommendation = (overrides == null ? List.<AnimeOverride>of() : overrides).stream()
                .filter(override -> AnimeOverrideService.DEFAULT_LOCALE.equals(override.getLocale()))
                .filter(override -> "beginner_recommendation".equals(override.getFieldName()))
                .map(AnimeOverride::getFieldValue)
                .findFirst()
                .orElse(null);

        return new BeginnerAnimeDto(AnimeSummaryDto.from(anime), recommendation);
    }
}
