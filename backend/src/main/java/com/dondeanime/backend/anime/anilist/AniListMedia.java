package com.dondeanime.backend.anime.anilist;

import java.util.List;

/**
 * Un anime individual tal como lo devuelve AniList.
 * Todos los campos pueden ser null (AniList es permisivo con datos incompletos).
 */
public record AniListMedia(
        Long id,
        AniListTitle title,
        AniListFuzzyDate startDate,
        AniListFuzzyDate endDate,
        Integer episodes,
        Integer duration,
        AniListStudioConnection studios,
        String format,
        String status,
        Integer averageScore,
        Integer popularity,
        String description,
        AniListCoverImage coverImage,
        String bannerImage,
        List<String> genres,
        String season,
        Integer seasonYear
) {}
