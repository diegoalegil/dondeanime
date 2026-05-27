package com.dondeanime.backend.curated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CuratedListSaveRequest(
        @Size(max = 180) String slug,
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 2_000) String description,
        @NotBlank @Size(max = 120) String owner,
        CuratedListVisibility visibility,
        CuratedListStatus status,
        Boolean premiumOnly
) {
}
