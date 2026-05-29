package com.dondeanime.backend.trakt;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class TraktWatchedService {

    private static final String PROVIDER = "trakt";

    private final UserWatchedAnimeRepository watchedAnimeRepository;

    public TraktWatchedService(UserWatchedAnimeRepository watchedAnimeRepository) {
        this.watchedAnimeRepository = watchedAnimeRepository;
    }

    public TraktWatchedResponse watched(String externalUserId) {
        if (externalUserId == null || externalUserId.isBlank()) {
            return new TraktWatchedResponse(List.of());
        }

        List<String> slugs = watchedAnimeRepository.findWatchedSlugs(
                PROVIDER,
                externalUserId.trim());
        return new TraktWatchedResponse(slugs);
    }
}
