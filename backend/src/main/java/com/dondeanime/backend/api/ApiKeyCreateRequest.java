package com.dondeanime.backend.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ApiKeyCreateRequest(
        @Email @NotBlank String ownerEmail,
        @NotBlank String tier
) {}
