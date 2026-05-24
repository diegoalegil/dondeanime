package com.dondeanime.backend.affiliate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AffiliateTrackRequest(
        @NotBlank String providerSlug,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$") String country,
        @NotBlank String animeSlug
) {}
