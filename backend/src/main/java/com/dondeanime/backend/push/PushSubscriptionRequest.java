package com.dondeanime.backend.push;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PushSubscriptionRequest(
        @NotBlank @Email String userEmail,
        @NotBlank String endpoint,
        @NotNull @Valid PushSubscriptionKeys keys,
        @NotBlank String countryIso) {
}
