package com.dondeanime.backend.anime;

import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Vista resumida de un anime para listados.
 *
 * No expone el id interno de BD, syncedAt, ni tmdbId (son detalles
 * internos del backend que el frontend no debe ver).
 */
@Schema(description = "Vista resumida de un anime para listados publicos")
public record AnimeSummaryDto(
        Long anilistId,
        String slug,
        String titleEnglish,
        String titleRomaji,
        String format,
        String status,
        Integer episodes,
        Integer episodeDuration,
        String studio,
        Integer year,
        Integer averageScore,
        Integer popularity,
        String coverImage,
        Set<String> genres,
        String season,
        Integer seasonYear
) {
    public static AnimeSummaryDto from(Anime a) {
        return new AnimeSummaryDto(
                a.getAnilistId(),
                a.getSlug(),
                a.getTitleEnglish(),
                a.getTitleRomaji(),
                a.getFormat(),
                a.getStatus(),
                a.getEpisodes(),
                a.getEpisodeDuration(),
                a.getStudio(),
                a.getStartYear(),
                a.getAverageScore(),
                a.getPopularity(),
                a.getCoverImage(),
                a.getGenres(),
                a.getSeason(),
                a.getSeasonYear()
        );
    }
}
