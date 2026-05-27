package com.dondeanime.backend.anime;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.dondeanime.backend.character.CharacterDto;
import com.dondeanime.backend.studio.StudioDto;

/**
 * Vista completa de un anime para la pagina de detalle.
 *
 * Incluye todo lo de AnimeSummaryDto mas descripcion larga, fechas
 * detalladas y banner. Sigue ocultando id interno, syncedAt y tmdbId.
 */
public record AnimeDetailDto(
        Long anilistId,
        String slug,
        String titleEnglish,
        String titleRomaji,
        String trailerYoutubeId,
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
        List<StudioDto> studios,
        String season,
        Integer seasonYear,
        List<CharacterDto> characters
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
                a.getTrailerYoutubeId(),
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
                a.getStudios().stream()
                        .map(StudioDto::from)
                        .toList(),
                a.getSeason(),
                a.getSeasonYear(),
                a.getCharacterRoles().stream()
                        .sorted((left, right) -> left.getCharacter().getName()
                                .compareToIgnoreCase(right.getCharacter().getName()))
                        .map(CharacterDto::from)
                        .toList()
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
