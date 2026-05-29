package com.dondeanime.backend.chat;

import com.dondeanime.backend.anime.AnimeSummaryDto;

public record ChatRecommendationDto(
        AnimeSummaryDto anime,
        String canonicalUrl,
        String explanation) {
}
