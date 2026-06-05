package com.dondeanime.backend.news;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return newsItemRepository
                .findByStatusOrderByPublishedAtDesc(NewsStatus.PUBLISHED, PageRequest.of(0, safeLimit))
                .stream()
                .map(NewsSummaryDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<NewsDetailDto> publishedBySlug(String slug) {
        return newsItemRepository.findBySlugAndStatus(slug, NewsStatus.PUBLISHED).map(NewsDetailDto::from);
    }

    @Transactional(readOnly = true)
    public List<NewsSummaryDto> publishedForAnime(Long animeId) {
        return newsItemRepository
                .findByAnimeIdAndStatusOrderByPublishedAtDesc(animeId, NewsStatus.PUBLISHED)
                .stream()
                .map(NewsSummaryDto::from)
                .toList();
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
}
