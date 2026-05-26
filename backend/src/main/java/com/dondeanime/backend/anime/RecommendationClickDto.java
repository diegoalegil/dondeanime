package com.dondeanime.backend.anime;

public record RecommendationClickDto(
        String sourceAnimeSlug,
        String targetAnimeSlug,
        Long clicks
) {}
