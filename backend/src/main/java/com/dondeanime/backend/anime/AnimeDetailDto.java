package com.dondeanime.backend.anime;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Vista completa de un anime para la página de detalle.
 *
 * Incluye todo lo de AnimeSummaryDto más descripción larga, fechas
 * detalladas y banner. Sigue ocultando id interno, syncedAt y tmdbId.
 */
@Schema(description = "Vista completa de una ficha publica de anime")
public record AnimeDetailDto(
        Long anilistId,
        String slug,
        String titleEnglish,
        String titleRomaji,
        String description,
        boolean descriptionTranslationPending,
        String format,
        String status,
        Integer episodes,
        Integer averageScore,
        Integer popularity,
        String coverImage,
        String bannerImage,
        Integer startYear,
        Integer startMonth,
        Integer startDay,
        Integer endYear,
        Integer endMonth,
        Integer endDay,
        Set<String> genres,
        String season,
        Integer seasonYear
) {
    public static AnimeDetailDto from(Anime a) {
        return from(a, List.of());
    }

    public static AnimeDetailDto from(Anime a, List<AnimeOverride> overrides) {
        Map<String, String> overrideByField = (overrides == null ? List.<AnimeOverride>of() : overrides).stream()
                .filter(override -> AnimeOverrideService.DEFAULT_LOCALE.equals(override.getLocale()))
                .collect(Collectors.toMap(
                        AnimeOverride::getFieldName,
                        AnimeOverride::getFieldValue,
                        (first, ignored) -> first));

        String overrideDescription = blankToNull(overrideByField.get("description"));
        String spanishDescription = blankToNull(a.getDescriptionEs());
        String originalDescription = blankToNull(a.getDescription());
        String publicDescription = firstNonBlank(overrideDescription, spanishDescription, originalDescription);
        boolean translationPending = overrideDescription == null
                && spanishDescription == null
                && originalDescription != null;

        return new AnimeDetailDto(
                a.getAnilistId(),
                a.getSlug(),
                overrideByField.getOrDefault("title_english", a.getTitleEnglish()),
                overrideByField.getOrDefault("title_romaji", a.getTitleRomaji()),
                publicDescription,
                translationPending,
                a.getFormat(),
                a.getStatus(),
                a.getEpisodes(),
                a.getAverageScore(),
                a.getPopularity(),
                a.getCoverImage(),
                a.getBannerImage(),
                a.getStartYear(),
                a.getStartMonth(),
                a.getStartDay(),
                a.getEndYear(),
                a.getEndMonth(),
                a.getEndDay(),
                a.getGenres(),
                a.getSeason(),
                a.getSeasonYear()
        );
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = blankToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
