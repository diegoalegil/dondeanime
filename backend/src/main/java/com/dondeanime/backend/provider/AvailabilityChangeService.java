package com.dondeanime.backend.provider;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.dondeanime.backend.anime.Anime;

@Service
public class AvailabilityChangeService {

    private static final String ADDED = "ADDED";
    private static final String REMOVED = "REMOVED";

    private final AvailabilityChangeEventRepository repository;

    public AvailabilityChangeService(AvailabilityChangeEventRepository repository) {
        this.repository = repository;
    }

    public int recordChanges(
            Anime anime,
            List<WatchProvider> existingProviders,
            List<WatchProvider> currentProviders,
            Instant changedAt) {
        Set<ProviderKey> existing = toKeys(existingProviders);
        Set<ProviderKey> current = toKeys(currentProviders);

        List<AvailabilityChangeEvent> events = currentProviders.stream()
                .filter(provider -> !existing.contains(ProviderKey.from(provider)))
                .map(provider -> event(anime, provider, ADDED, changedAt))
                .toList();

        List<AvailabilityChangeEvent> removedEvents = existingProviders.stream()
                .filter(provider -> !current.contains(ProviderKey.from(provider)))
                .map(provider -> event(anime, provider, REMOVED, changedAt))
                .toList();

        if (events.isEmpty() && removedEvents.isEmpty()) {
            return 0;
        }

        List<AvailabilityChangeEvent> allEvents = new java.util.ArrayList<>(events);
        allEvents.addAll(removedEvents);
        repository.saveAll(allEvents);
        return allEvents.size();
    }

    private static Set<ProviderKey> toKeys(List<WatchProvider> providers) {
        Set<ProviderKey> keys = new HashSet<>();
        for (WatchProvider provider : providers) {
            keys.add(ProviderKey.from(provider));
        }
        return keys;
    }

    private static AvailabilityChangeEvent event(
            Anime anime,
            WatchProvider provider,
            String changeType,
            Instant changedAt) {
        AvailabilityChangeEvent event = new AvailabilityChangeEvent();
        event.setAnimeId(anime.getId());
        event.setAnimeSlug(anime.getSlug());
        event.setCountryCode(provider.getCountryCode());
        event.setProviderName(provider.getProviderName());
        event.setProviderType(provider.getProviderType());
        event.setChangeType(changeType);
        event.setChangedAt(changedAt);
        return event;
    }

    private record ProviderKey(String countryCode, String providerName, String providerType) {

        static ProviderKey from(WatchProvider provider) {
            return new ProviderKey(
                    provider.getCountryCode(),
                    provider.getProviderName(),
                    provider.getProviderType());
        }
    }
}
