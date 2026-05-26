package com.dondeanime.backend.anime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

class RecommendationTrackingServiceTest {

    private final RecommendationEventRepository eventRepository = org.mockito.Mockito.mock(
            RecommendationEventRepository.class);
    private final RecommendationTrackingService service = new RecommendationTrackingService(eventRepository);

    @Test
    void trackClickNormalizesAndStoresRecommendationEvent() {
        service.trackClick(new RecommendationTrackRequest(" Attack-On-Titan ", " Vinland-Saga "));

        ArgumentCaptor<RecommendationEvent> eventCaptor = ArgumentCaptor.forClass(RecommendationEvent.class);
        verify(eventRepository).save(eventCaptor.capture());

        RecommendationEvent event = eventCaptor.getValue();
        assertThat(event.getSourceAnimeSlug()).isEqualTo("attack-on-titan");
        assertThat(event.getTargetAnimeSlug()).isEqualTo("vinland-saga");
        assertThat(event.getClickedAt()).isNotNull();
    }

    @Test
    void trackClickSkipsSelfRecommendation() {
        service.trackClick(new RecommendationTrackRequest("attack-on-titan", "attack-on-titan"));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void topRecommendationClicksMapsProjection() {
        RecommendationEventRepository.RecommendationClickProjection projection = projection(
                "attack-on-titan",
                "vinland-saga",
                3L);
        when(eventRepository.findTopRecommendationClicks(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(projection));

        List<RecommendationClickDto> topClicks = service.topRecommendationClicks30Days();

        assertThat(topClicks).containsExactly(new RecommendationClickDto(
                "attack-on-titan",
                "vinland-saga",
                3L));
    }

    private static RecommendationEventRepository.RecommendationClickProjection projection(
            String sourceAnimeSlug,
            String targetAnimeSlug,
            Long clicks) {
        return new RecommendationEventRepository.RecommendationClickProjection() {
            @Override
            public String getSourceAnimeSlug() {
                return sourceAnimeSlug;
            }

            @Override
            public String getTargetAnimeSlug() {
                return targetAnimeSlug;
            }

            @Override
            public Long getClicks() {
                return clicks;
            }
        };
    }
}
