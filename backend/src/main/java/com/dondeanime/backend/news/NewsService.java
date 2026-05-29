package com.dondeanime.backend.news;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NewsService {

    private static final int MAX_LIMIT = 100;

    private final NewsItemRepository newsItemRepository;

    public NewsService(NewsItemRepository newsItemRepository) {
        this.newsItemRepository = newsItemRepository;
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
}
