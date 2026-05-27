package com.dondeanime.backend.embedding;

import java.util.List;

public record AnimeSearchDocument(
        String slug,
        String title,
        String spanishSynopsis,
        List<String> genres,
        String season,
        Integer seasonYear,
        Integer averageScore,
        List<CountryPlatforms> availability,
        String canonicalUrl) {

    public record CountryPlatforms(
            String countryCode,
            List<String> platforms) {
    }
}
