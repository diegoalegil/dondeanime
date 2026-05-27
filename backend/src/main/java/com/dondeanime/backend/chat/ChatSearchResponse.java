package com.dondeanime.backend.chat;

import java.util.List;

public record ChatSearchResponse(
        String answer,
        List<ChatRecommendationDto> recommendations) {
}
