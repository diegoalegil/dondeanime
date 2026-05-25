package com.dondeanime.backend.admin;

import com.dondeanime.backend.anime.AnimeMatchingService;

public record AnimeRematchResponse(
        String slug,
        boolean matched
) {
    public static AnimeRematchResponse from(AnimeMatchingService.RematchResult result) {
        return new AnimeRematchResponse(result.slug(), result.matched());
    }
}
