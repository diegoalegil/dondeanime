package com.dondeanime.backend.curated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CuratedListMetricRequest(
        @NotBlank @Size(max = 180) String listSlug,
        @Size(max = 180) String animeSlug
) {
}
