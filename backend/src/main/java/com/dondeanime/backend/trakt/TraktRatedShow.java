package com.dondeanime.backend.trakt;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TraktRatedShow(
        Integer rating,
        @JsonProperty("rated_at") Instant ratedAt,
        TraktShow show) {
}
