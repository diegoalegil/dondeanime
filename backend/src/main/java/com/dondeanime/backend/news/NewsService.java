package com.dondeanime.backend.news;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;

@Service
public class NewsService {

    private static final int MAX_LIMIT = 100;

    private final NewsItemRepository newsItemRepository;
    private final AnimeRepository animeRepository;

    public NewsService(NewsItemRepository newsItemRepository, AnimeRepository animeRepository) {
        this.newsItemRepository = newsItemRepository;
        this.animeRepository = animeRepository;
    }

    @Transactional(readOnly = true)
    public List<NewsSummaryDto> latestPublished(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        List<NewsItem> items = newsItemRepository
                .findByStatusOrderByPublishedAtDesc(NewsStatus.PUBLISHED, PageRequest.of(0, safeLimit));
        return toSummaries(items);
    }

    /**
     * Todos los slugs publicados, sin límite: el frontend los necesita en
     * getStaticPaths para construir CADA artículo (latestPublished capa a
     * {@value MAX_LIMIT} y dejaría artículos antiguos en 404 al superarlo).
     */
    @Transactional(readOnly = true)
    public List<String> publishedSlugs() {
        return newsItemRepository.findSlugsByStatus(NewsStatus.PUBLISHED);
    }

    @Transactional(readOnly = true)
    public Optional<NewsDetailDto> publishedBySlug(String slug) {
        return newsItemRepository.findBySlugAndStatus(slug, NewsStatus.PUBLISHED).map(NewsDetailDto::from);
    }

    @Transactional(readOnly = true)
    public List<NewsSummaryDto> publishedForAnime(Long animeId) {
        return toSummaries(newsItemRepository
                .findByAnimeIdAndStatusOrderByPublishedAtDesc(animeId, NewsStatus.PUBLISHED));
    }

    /**
     * Noticias PUBLISHED de un anime identificado por su slug público. El
     * frontend solo conoce slug/anilistId (no el id interno que referencia
     * {@code news_item.anime_id}), así que resolvemos slug → id aquí. Si el
     * slug no existe devolvemos lista vacía (no 404: la ficha simplemente no
     * muestra la sección de noticias).
     */
    @Transactional(readOnly = true)
    public List<NewsSummaryDto> publishedForAnimeSlug(String slug) {
        return animeRepository.findBySlug(slug)
                .map(anime -> publishedForAnime(anime.getId()))
                .orElseGet(List::of);
    }

    private List<NewsSummaryDto> toSummaries(List<NewsItem> items) {
        Map<Long, Anime> animeById = animeById(items);
        return items.stream()
                .map(item -> NewsSummaryDto.from(item,
                        Optional.ofNullable(item.getAnimeId())
                                .map(animeById::get)
                                .map(Anime::getSlug)
                                .orElse(null)))
                .toList();
    }

    private Map<Long, Anime> animeById(List<NewsItem> items) {
        Set<Long> animeIds = items.stream()
                .map(NewsItem::getAnimeId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (animeIds.isEmpty()) {
            return Map.of();
        }
        return animeRepository.findAllById(animeIds).stream()
                .collect(Collectors.toMap(Anime::getId, Function.identity()));
    }
}
