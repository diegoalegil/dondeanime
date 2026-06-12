package com.dondeanime.backend.news;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsItemRepository extends JpaRepository<NewsItem, Long> {

    List<NewsItem> findByStatusOrderByPublishedAtDesc(NewsStatus status, Pageable pageable);

    @Query("select n.slug from NewsItem n where n.status = :status order by n.publishedAt desc")
    List<String> findSlugsByStatus(@Param("status") NewsStatus status);

    List<NewsItem> findByStatusOrderByFetchedAtAsc(NewsStatus status, Pageable pageable);

    List<NewsItem> findByAnimeIdAndStatusOrderByPublishedAtDesc(Long animeId, NewsStatus status);

    Optional<NewsItem> findBySlugAndStatus(String slug, NewsStatus status);

    boolean existsBySlug(String slug);

    boolean existsByDedupKey(String dedupKey);

    /** Cupo diario del LLM: ítems redactados desde un instante (ver NewsProcessingService). */
    long countByLlmTokensUsedIsNotNullAndUpdatedAtGreaterThanEqual(java.time.Instant since);
}
