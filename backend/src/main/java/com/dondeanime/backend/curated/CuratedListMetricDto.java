package com.dondeanime.backend.curated;

public record CuratedListMetricDto(
        String listSlug,
        Long events
) {
}
