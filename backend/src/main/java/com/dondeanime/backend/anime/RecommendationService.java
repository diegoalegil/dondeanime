package com.dondeanime.backend.anime;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.dondeanime.backend.provider.WatchProviderRepository;
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
    private final WatchProviderRepository watchProviderRepository;
    private final Cache<RecommendationKey, List<Anime>> cache;

    @Autowired
    public RecommendationService(
            AnimeRepository animeRepository,
            WatchProviderRepository watchProviderRepository) {
        this(
                animeRepository,
                watchProviderRepository,
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofHours(24))
                        .maximumSize(5_000)
                        .build());
    }

    RecommendationService(
            AnimeRepository animeRepository,
            WatchProviderRepository watchProviderRepository,
            Cache<RecommendationKey, List<Anime>> cache) {
        this.animeRepository = animeRepository;
        this.watchProviderRepository = watchProviderRepository;
        this.cache = cache;
    }

    public List<Anime> findSimilar(Long animeId, int limit) {
        if (animeId == null || limit <= 0) {
            return List.of();
        }

        return cache.get(new RecommendationKey(animeId, limit), this::findSimilarUncached);
    }

    public List<Anime> findSimilar(Long animeId, int limit, List<String> watchedSlugs) {
        if (animeId == null || limit <= 0) {
            return List.of();
        }

        Set<String> watched = normalizeSlugs(watchedSlugs);
        if (watched.isEmpty()) {
            return findSimilar(animeId, limit);
        }

        int expandedLimit = Math.max(limit * 3, limit + watched.size());
        List<Anime> candidates = cache.get(new RecommendationKey(animeId, expandedLimit), this::findSimilarUncached);
        return personalize(candidates, watched, limit);
    }

    private List<Anime> findSimilarUncached(RecommendationKey key) {
        Optional<Anime> source = animeRepository.findByIdWithGenres(key.animeId());
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

        List<Long> orderedIds = recommendations.values().stream()
                .map(Anime::getId)
                .limit(key.limit())
                .toList();
        return reloadWithGenres(orderedIds);
    }

    /**
     * Recarga las recomendaciones por id trayendo genres (vía EntityGraph) y
     * preservando el orden. Necesario porque las queries findSimilarBy* NO traen
     * genres: al cachear y serializar fuera de sesion (open-in-view=false),
     * AnimeSummaryDto leeria anime.getGenres() lazy y lanzaria
     * LazyInitializationException (rompia GET /api/anime/{slug}/similar con 500).
     */
    private List<Anime> reloadWithGenres(List<Long> orderedIds) {
        if (orderedIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Anime> byId = animeRepository.findByIdInWithGenres(orderedIds).stream()
                .collect(Collectors.toMap(Anime::getId, Function.identity()));
        return orderedIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<Anime> personalize(List<Anime> candidates, Set<String> watchedSlugs, int limit) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        Map<String, Integer> originalOrder = new HashMap<>();
        for (int i = 0; i < candidates.size(); i++) {
            originalOrder.put(normalizeSlug(candidates.get(i).getSlug()), i);
        }

        Map<String, Long> preferredGenres = preferredGenres(watchedSlugs);
        Map<String, Set<String>> providersByAnimeSlug = providersByAnimeSlug(candidates, watchedSlugs);
        Map<String, Long> preferredProviders = preferredProviders(watchedSlugs, providersByAnimeSlug);

        return candidates.stream()
                .filter(candidate -> !watchedSlugs.contains(normalizeSlug(candidate.getSlug())))
                .sorted(Comparator
                        .comparingLong((Anime candidate) -> preferenceScore(
                                candidate,
                                preferredGenres,
                                preferredProviders,
                                providersByAnimeSlug))
                        .reversed()
                        .thenComparingInt(candidate -> originalOrder.getOrDefault(
                                normalizeSlug(candidate.getSlug()),
                                Integer.MAX_VALUE)))
                .limit(limit)
                .toList();
    }

    private Map<String, Long> preferredGenres(Set<String> watchedSlugs) {
        return animeRepository.findBySlugInWithGenres(watchedSlugs).stream()
                .flatMap(RecommendationService::genresOf)
                .map(RecommendationService::normalizeSlug)
                .filter(RecommendationService::hasText)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private Map<String, Set<String>> providersByAnimeSlug(List<Anime> candidates, Set<String> watchedSlugs) {
        Set<String> slugs = new HashSet<>(watchedSlugs);
        candidates.stream()
                .map(Anime::getSlug)
                .map(RecommendationService::normalizeSlug)
                .filter(RecommendationService::hasText)
                .forEach(slugs::add);

        if (slugs.isEmpty()) {
            return Map.of();
        }

        return watchProviderRepository.findProviderSlugsByAnimeSlugs(slugs).stream()
                .filter(row -> hasText(row.getAnimeSlug()) && hasText(row.getProviderSlug()))
                .collect(Collectors.groupingBy(
                        row -> normalizeSlug(row.getAnimeSlug()),
                        Collectors.mapping(
                                row -> normalizeSlug(row.getProviderSlug()),
                                Collectors.toSet())));
    }

    private static Map<String, Long> preferredProviders(
            Set<String> watchedSlugs,
            Map<String, Set<String>> providersByAnimeSlug) {
        return watchedSlugs.stream()
                .flatMap(slug -> providersByAnimeSlug.getOrDefault(slug, Set.of()).stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private static long preferenceScore(
            Anime candidate,
            Map<String, Long> preferredGenres,
            Map<String, Long> preferredProviders,
            Map<String, Set<String>> providersByAnimeSlug) {
        long genreScore = genresOf(candidate)
                .map(RecommendationService::normalizeSlug)
                .mapToLong(genre -> preferredGenres.getOrDefault(genre, 0L))
                .sum();
        long providerScore = providersByAnimeSlug
                .getOrDefault(normalizeSlug(candidate.getSlug()), Set.of())
                .stream()
                .mapToLong(provider -> preferredProviders.getOrDefault(provider, 0L))
                .sum();
        return (genreScore * 3) + (providerScore * 2);
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

    private static Stream<String> genresOf(Anime anime) {
        if (anime == null || anime.getGenres() == null) {
            return Stream.empty();
        }
        return anime.getGenres().stream();
    }

    private static Set<String> normalizeSlugs(List<String> slugs) {
        if (slugs == null) {
            return Set.of();
        }

        return slugs.stream()
                .map(RecommendationService::normalizeSlug)
                .filter(RecommendationService::hasText)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private static String normalizeSlug(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record RecommendationKey(Long animeId, int limit) {
    }
}
