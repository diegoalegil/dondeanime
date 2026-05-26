package com.dondeanime.backend.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dondeanime.backend.anime.Anime;

class AvailabilityChangeServiceTest {

    private final AvailabilityChangeEventRepository repository =
            org.mockito.Mockito.mock(AvailabilityChangeEventRepository.class);
    private final AvailabilityChangeService service = new AvailabilityChangeService(repository);

    @Test
    void recordChangesStoresAddedAndRemovedProviders() {
        Anime anime = anime();
        WatchProvider existing = provider("ES", "Netflix", "FLATRATE");
        WatchProvider current = provider("ES", "Crunchyroll", "FLATRATE");

        int changes = service.recordChanges(
                anime,
                List.of(existing),
                List.of(current),
                Instant.parse("2026-05-25T12:00:00Z"));

        assertThat(changes).isEqualTo(2);
        verify(repository).saveAll(org.mockito.ArgumentMatchers.argThat(events -> {
            List<AvailabilityChangeEvent> saved = new java.util.ArrayList<>();
            events.forEach(saved::add);
            return saved.size() == 2
                    && saved.stream().anyMatch(event -> "ADDED".equals(event.getChangeType())
                            && "Crunchyroll".equals(event.getProviderName()))
                    && saved.stream().anyMatch(event -> "REMOVED".equals(event.getChangeType())
                            && "Netflix".equals(event.getProviderName()));
        }));
    }

    @Test
    void recordChangesSkipsWhenProviderSetIsStable() {
        WatchProvider provider = provider("ES", "Crunchyroll", "FLATRATE");

        int changes = service.recordChanges(
                anime(),
                List.of(provider),
                List.of(provider),
                Instant.parse("2026-05-25T12:00:00Z"));

        assertThat(changes).isZero();
    }

    private static Anime anime() {
        Anime anime = new Anime();
        anime.setId(1L);
        anime.setSlug("frieren");
        return anime;
    }

    private static WatchProvider provider(String country, String name, String type) {
        WatchProvider provider = new WatchProvider();
        provider.setAnimeId(1L);
        provider.setCountryCode(country);
        provider.setProviderName(name);
        provider.setProviderType(type);
        provider.setUpdatedAt(Instant.parse("2026-05-25T12:00:00Z"));
        return provider;
    }
}
