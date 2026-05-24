package com.dondeanime.backend.subscription;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SubscriptionRequest(
        @NotBlank @Email String email,
        @NotBlank String animeSlug,
        @NotBlank String country
) {}
