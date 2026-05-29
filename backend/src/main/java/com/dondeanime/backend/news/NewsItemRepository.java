package com.dondeanime.backend.news;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsItemRepository extends JpaRepository<NewsItem, Long> {

    List<NewsItem> findByStatusOrderByPublishedAtDesc(NewsStatus status, Pageable pageable);

    List<NewsItem> findByAnimeIdAndStatusOrderByPublishedAtDesc(Long animeId, NewsStatus status);

    Optional<NewsItem> findBySlugAndStatus(String slug, NewsStatus status);

    Optional<NewsItem> findBySlug(String slug);

    boolean existsBySourceUrl(String sourceUrl);
}
