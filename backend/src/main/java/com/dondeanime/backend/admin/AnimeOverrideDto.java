package com.dondeanime.backend.admin;

import java.time.Instant;

import com.dondeanime.backend.anime.AnimeOverride;

public record AnimeOverrideDto(
        String fieldName,
        String fieldValue,
        String locale,
        Instant updatedAt,
        String updatedBy,
        String originalValue
) {
    public static AnimeOverrideDto from(AnimeOverride override, String originalValue) {
        return new AnimeOverrideDto(
                override.getFieldName(),
                override.getFieldValue(),
                override.getLocale(),
                override.getUpdatedAt(),
                override.getUpdatedBy(),
                originalValue);
    }
}
