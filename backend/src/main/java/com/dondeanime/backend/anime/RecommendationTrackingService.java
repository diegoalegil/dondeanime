package com.dondeanime.backend.anime;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationTrackingService {

    private final RecommendationEventRepository eventRepository;

    public RecommendationTrackingService(RecommendationEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public void trackClick(RecommendationTrackRequest request) {
        String sourceAnimeSlug = normalizeSlug(request.sourceAnimeSlug());
        String targetAnimeSlug = normalizeSlug(request.targetAnimeSlug());
        if (sourceAnimeSlug.isBlank() || targetAnimeSlug.isBlank() || sourceAnimeSlug.equals(targetAnimeSlug)) {
            return;
        }

        RecommendationEvent event = new RecommendationEvent();
        event.setSourceAnimeSlug(sourceAnimeSlug);
        event.setTargetAnimeSlug(targetAnimeSlug);
        event.setClickedAt(Instant.now());
        eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<RecommendationClickDto> topRecommendationClicks30Days() {
        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
        return eventRepository.findTopRecommendationClicks(since, PageRequest.of(0, 10))
                .stream()
                .map(row -> new RecommendationClickDto(
                        row.getSourceAnimeSlug(),
                        row.getTargetAnimeSlug(),
                        row.getClicks()))
                .toList();
    }

    private static String normalizeSlug(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
