package com.dondeanime.backend.provider;

import java.util.List;

import com.dondeanime.backend.anime.Anime;

public record ProviderAddedEvent(
        Anime anime,
        String countryCode,
        List<WatchProvider> providers) {

    public ProviderAddedEvent {
        providers = List.copyOf(providers);
    }
}
