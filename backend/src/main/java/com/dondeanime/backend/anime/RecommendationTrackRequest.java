package com.dondeanime.backend.anime;

import jakarta.validation.constraints.NotBlank;

public record RecommendationTrackRequest(
        @NotBlank String sourceAnimeSlug,
        @NotBlank String targetAnimeSlug
) {}
