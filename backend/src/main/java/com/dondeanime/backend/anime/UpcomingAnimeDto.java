package com.dondeanime.backend.anime;

import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Vista para listados de próximos estrenos.
 *
 * Incluye fecha completa porque aquí sí importa el día exacto. Mantiene
 * fuera id interno, tmdbId y syncedAt igual que AnimeSummaryDto.
 */
@Schema(description = "Anime proximo a estrenarse con fecha completa")
public record UpcomingAnimeDto(
        Long anilistId,
        String slug,
        String titleEnglish,
        String titleRomaji,
        String format,
        String status,
        Integer episodes,
        Integer year,
        Integer startYear,
        Integer startMonth,
        Integer startDay,
        Integer averageScore,
        Integer popularity,
        String coverImage,
        Set<String> genres,
        String season,
        Integer seasonYear
) {
    public static UpcomingAnimeDto from(Anime a) {
        return new UpcomingAnimeDto(
                a.getAnilistId(),
                a.getSlug(),
                a.getTitleEnglish(),
                a.getTitleRomaji(),
                a.getFormat(),
                a.getStatus(),
                a.getEpisodes(),
                a.getStartYear(),
                a.getStartYear(),
                a.getStartMonth(),
                a.getStartDay(),
                a.getAverageScore(),
                a.getPopularity(),
                a.getCoverImage(),
                a.getGenres(),
                a.getSeason(),
                a.getSeasonYear()
        );
    }
}
