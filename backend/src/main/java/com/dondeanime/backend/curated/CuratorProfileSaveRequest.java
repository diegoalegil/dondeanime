package com.dondeanime.backend.curated;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CuratorProfileSaveRequest(
        @Email @NotBlank @Size(max = 180) String email,
        @NotBlank @Size(max = 120) String displayName
) {
}
