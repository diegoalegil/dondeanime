package com.dondeanime.backend.trakt;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TraktWatchedShow(
        Integer plays,
        @JsonProperty("last_watched_at") Instant lastWatchedAt,
        TraktShow show) {
}
