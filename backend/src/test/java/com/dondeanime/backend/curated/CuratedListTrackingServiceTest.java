package com.dondeanime.backend.curated;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CuratedListTrackingServiceTest {

    private final CuratedListMetricRepository repository = org.mockito.Mockito.mock(CuratedListMetricRepository.class);
    private final CuratedListTrackingService service = new CuratedListTrackingService(
            repository,
            Clock.fixed(Instant.parse("2026-05-27T10:00:00Z"), ZoneOffset.UTC));

    @Test
    void trackViewNormalizesAndStoresEvent() {
        service.track(new CuratedListMetricRequest(" Anime-Para-Empezar ", null), CuratedListMetricType.VIEW);

        ArgumentCaptor<CuratedListMetricEvent> captor = ArgumentCaptor.forClass(CuratedListMetricEvent.class);
        verify(repository).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getListSlug()).isEqualTo("anime-para-empezar");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getEventType()).isEqualTo(CuratedListMetricType.VIEW);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getOccurredAt())
                .isEqualTo(Instant.parse("2026-05-27T10:00:00Z"));
    }

    @Test
    void animeClickRequiresAnimeSlug() {
        service.track(new CuratedListMetricRequest("anime-para-empezar", null), CuratedListMetricType.ANIME_CLICK);

        verify(repository, never()).save(any());
    }

    @Test
    void trackConversionStoresPremiumConversionEvent() {
        service.trackConversion(" Anime-Para-Empezar ");

        ArgumentCaptor<CuratedListMetricEvent> captor = ArgumentCaptor.forClass(CuratedListMetricEvent.class);
        verify(repository).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getListSlug()).isEqualTo("anime-para-empezar");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getEventType())
                .isEqualTo(CuratedListMetricType.PREMIUM_CONVERSION);
    }
}
