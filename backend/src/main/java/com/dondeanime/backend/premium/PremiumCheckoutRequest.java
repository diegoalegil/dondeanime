package com.dondeanime.backend.premium;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PremiumCheckoutRequest(
        @NotBlank @Email String email) {
}
