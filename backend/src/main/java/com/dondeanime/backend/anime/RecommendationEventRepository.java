package com.dondeanime.backend.anime;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RecommendationEventRepository extends JpaRepository<RecommendationEvent, Long> {

    @Query("""
            SELECT e.sourceAnimeSlug AS sourceAnimeSlug,
                   e.targetAnimeSlug AS targetAnimeSlug,
                   COUNT(e) AS clicks
              FROM RecommendationEvent e
             WHERE e.clickedAt >= :since
             GROUP BY e.sourceAnimeSlug, e.targetAnimeSlug
             ORDER BY COUNT(e) DESC, e.sourceAnimeSlug ASC, e.targetAnimeSlug ASC
            """)
    List<RecommendationClickProjection> findTopRecommendationClicks(Instant since, Pageable pageable);

    interface RecommendationClickProjection {
        String getSourceAnimeSlug();

        String getTargetAnimeSlug();

        Long getClicks();
    }
}
