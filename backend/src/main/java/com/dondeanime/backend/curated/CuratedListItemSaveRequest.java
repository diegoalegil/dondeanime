package com.dondeanime.backend.curated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CuratedListItemSaveRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$")
        @Size(max = 180)
        String animeSlug,

        @Size(max = 600)
        String note
) {
}
