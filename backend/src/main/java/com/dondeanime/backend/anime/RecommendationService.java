package com.dondeanime.backend.anime;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Service
public class RecommendationService {

    private static final int MIN_SCORE = 70;
    private static final int MIN_TAG_RANK = 70;
    private static final int GENRE_LIMIT = 10;
    private static final int STUDIO_LIMIT = 5;
    private static final int TAG_LIMIT = 10;

    private final AnimeRepository animeRepository;
    private final Cache<RecommendationKey, List<Anime>> cache;

    @Autowired
    public RecommendationService(AnimeRepository animeRepository) {
        this(
                animeRepository,
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofHours(24))
                        .maximumSize(5_000)
                        .build());
    }

    RecommendationService(
            AnimeRepository animeRepository,
            Cache<RecommendationKey, List<Anime>> cache) {
        this.animeRepository = animeRepository;
        this.cache = cache;
    }

    public List<Anime> findSimilar(Long animeId, int limit) {
        if (animeId == null || limit <= 0) {
            return List.of();
        }

        return cache.get(new RecommendationKey(animeId, limit), this::findSimilarUncached);
    }

    private List<Anime> findSimilarUncached(RecommendationKey key) {
        Optional<Anime> source = animeRepository.findById(key.animeId());
        if (source.isEmpty()) {
            return List.of();
        }

        Anime anime = source.get();
        LinkedHashMap<Long, Anime> recommendations = new LinkedHashMap<>();

        primaryGenre(anime).ifPresent(genre -> addCandidates(
                recommendations,
                animeRepository.findSimilarByPrimaryGenre(
                        anime.getId(),
                        genre,
                        MIN_SCORE,
                        PageRequest.of(0, GENRE_LIMIT))));

        if (hasText(anime.getPrimaryStudio())) {
            addCandidates(
                    recommendations,
                    animeRepository.findSimilarByPrimaryStudio(
                            anime.getId(),
                            anime.getPrimaryStudio(),
                            MIN_SCORE,
                            PageRequest.of(0, STUDIO_LIMIT)));
        }

        addCandidates(
                recommendations,
                animeRepository.findSimilarBySharedHighRankTags(
                        anime.getId(),
                        MIN_SCORE,
                        MIN_TAG_RANK,
                        PageRequest.of(0, TAG_LIMIT)));

        return recommendations.values().stream()
                .limit(key.limit())
                .toList();
    }

    private static void addCandidates(LinkedHashMap<Long, Anime> recommendations, List<Anime> candidates) {
        if (candidates == null) {
            return;
        }

        for (Anime candidate : candidates) {
            if (candidate.getId() != null) {
                recommendations.putIfAbsent(candidate.getId(), candidate);
            }
        }
    }

    private static Optional<String> primaryGenre(Anime anime) {
        if (anime.getGenres() == null) {
            return Optional.empty();
        }

        return anime.getGenres().stream()
                .filter(RecommendationService::hasText)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .findFirst();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record RecommendationKey(Long animeId, int limit) {
    }
}
