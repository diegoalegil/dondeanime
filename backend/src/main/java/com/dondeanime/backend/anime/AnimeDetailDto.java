package com.dondeanime.backend.anime;

import java.util.Set;

/**
 * Vista completa de un anime para la página de detalle.
 *
 * Incluye todo lo de AnimeSummaryDto más descripción larga, fechas
 * detalladas y banner. Sigue ocultando id interno, syncedAt y tmdbId.
 */
public record AnimeDetailDto(
        Long anilistId,
        String slug,
        String titleEnglish,
        String titleRomaji,
        String description,
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
        return new AnimeDetailDto(
                a.getAnilistId(),
                a.getSlug(),
                a.getTitleEnglish(),
                a.getTitleRomaji(),
                a.getDescription(),
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
}
