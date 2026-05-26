package com.dondeanime.backend.anime;

import java.text.Normalizer;

public record StudioSummaryDto(
        String name,
        String slug,
        Long animeCount
) {
    public static StudioSummaryDto of(String studio, Long animeCount) {
        return new StudioSummaryDto(studio, slugify(studio), animeCount);
    }

    public static String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        normalized = normalized.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
        if (normalized.startsWith("-")) normalized = normalized.substring(1);
        if (normalized.endsWith("-")) normalized = normalized.substring(0, normalized.length() - 1);
        return normalized;
    }
}
