package com.dondeanime.backend.trakt;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TraktWatchedService {

    private static final String PROVIDER = "trakt";

    private final UserWatchedAnimeRepository watchedAnimeRepository;
    private final boolean enabled;

    public TraktWatchedService(
            UserWatchedAnimeRepository watchedAnimeRepository,
            @Value("${trakt.enabled:false}") boolean enabled) {
        this.watchedAnimeRepository = watchedAnimeRepository;
        this.enabled = enabled;
    }

    public TraktWatchedResponse watched(String externalUserId) {
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "trakt_disabled");
        }
        if (externalUserId == null || externalUserId.isBlank()) {
            return new TraktWatchedResponse(List.of());
        }

        List<String> slugs = watchedAnimeRepository.findWatchedSlugs(
                PROVIDER,
                externalUserId.trim());
        return new TraktWatchedResponse(slugs);
    }
}
