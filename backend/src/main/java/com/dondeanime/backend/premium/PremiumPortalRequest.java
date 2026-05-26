package com.dondeanime.backend.premium;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PremiumPortalRequest(
        @NotBlank @Email String email) {
}
