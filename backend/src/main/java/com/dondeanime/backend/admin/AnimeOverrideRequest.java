package com.dondeanime.backend.admin;

import jakarta.validation.constraints.NotBlank;

public record AnimeOverrideRequest(
        @NotBlank String fieldName,
        @NotBlank String fieldValue,
        String locale
) {}
