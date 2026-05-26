package com.dondeanime.backend.admin.auth;

import jakarta.validation.constraints.NotBlank;

public record AdminTotpVerifyRequest(
        @NotBlank String secret,
        @NotBlank String code) {
}
