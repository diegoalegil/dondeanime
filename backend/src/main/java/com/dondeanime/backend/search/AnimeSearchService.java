package com.dondeanime.backend.search;

import java.util.List;

import org.springframework.stereotype.Service;

import com.dondeanime.backend.anime.AnimeRepository;
import com.dondeanime.backend.anime.AnimeSummaryDto;

@Service
public class AnimeSearchService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 25;

    private final AnimeRepository repository;

    public AnimeSearchService(AnimeRepository repository) {
        this.repository = repository;
    }

    public List<AnimeSummaryDto> search(String query, Integer limit) {
        String normalizedQuery = normalizeQuery(query);
        if (normalizedQuery == null) {
            return List.of();
        }

        int safeLimit = safeLimit(limit);
        return repository.findBySearchVectorMatching(normalizedQuery).stream()
                .limit(safeLimit)
                .map(AnimeSummaryDto::from)
                .toList();
    }

    private static String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return query.trim();
    }

    private static int safeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }
}
