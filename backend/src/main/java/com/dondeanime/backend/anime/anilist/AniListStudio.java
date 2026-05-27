package com.dondeanime.backend.anime.anilist;

public record AniListStudio(
        Long id,
        String name,
        Boolean isAnimationStudio
) {}
