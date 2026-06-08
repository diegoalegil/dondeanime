package com.dondeanime.backend.search;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.dondeanime.backend.anime.Anime;
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

        List<Long> ids = repository.findIdsBySearchVectorMatching(normalizedQuery, safeLimit(limit));
        if (ids.isEmpty()) {
            return List.of();
        }

        Map<Long, Anime> animeById = repository.findByIdInWithGenres(ids).stream()
                .collect(Collectors.toMap(Anime::getId, Function.identity()));

        return ids.stream()
                .map(animeById::get)
                .filter(Objects::nonNull)
                .map(AnimeSummaryDto::from)
                .toList();
    }

    private static String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return query.trim()
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ");
    }

    private static int safeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }
}
