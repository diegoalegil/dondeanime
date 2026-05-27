package com.dondeanime.backend.curated;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CuratedListTrackingService {

    private final CuratedListMetricRepository repository;
    private final Clock clock;

    @Autowired
    public CuratedListTrackingService(CuratedListMetricRepository repository) {
        this(repository, Clock.systemUTC());
    }

    CuratedListTrackingService(CuratedListMetricRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void track(CuratedListMetricRequest request, CuratedListMetricType type) {
        String listSlug = normalizeSlug(request.listSlug());
        String animeSlug = normalizeSlug(request.animeSlug());
        if (listSlug.isBlank()) {
            return;
        }
        if (type == CuratedListMetricType.ANIME_CLICK && animeSlug.isBlank()) {
            return;
        }

        CuratedListMetricEvent event = new CuratedListMetricEvent();
        event.setListSlug(listSlug);
        event.setAnimeSlug(animeSlug.isBlank() ? null : animeSlug);
        event.setEventType(type);
        event.setOccurredAt(Instant.now(clock));
        repository.save(event);
    }

    @Transactional
    public void trackConversion(String listSlug) {
        String normalizedListSlug = normalizeSlug(listSlug);
        if (normalizedListSlug.isBlank()) {
            return;
        }

        CuratedListMetricEvent event = new CuratedListMetricEvent();
        event.setListSlug(normalizedListSlug);
        event.setEventType(CuratedListMetricType.PREMIUM_CONVERSION);
        event.setOccurredAt(Instant.now(clock));
        repository.save(event);
    }

    static String normalizeSlug(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
