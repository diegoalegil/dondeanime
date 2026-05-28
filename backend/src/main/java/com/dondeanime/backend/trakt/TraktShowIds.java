package com.dondeanime.backend.trakt;

public record TraktShowIds(
        Long trakt,
        String slug,
        String imdb,
        Long tmdb) {
}
