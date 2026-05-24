package com.dondeanime.backend.affiliate;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AffiliateClickEventRepository extends JpaRepository<AffiliateClickEvent, Long> {

    long countByClickedAtAfter(Instant since);

    @Query("""
            SELECT e.animeSlug AS animeSlug, COUNT(e) AS clicks
              FROM AffiliateClickEvent e
             WHERE e.clickedAt >= :since
             GROUP BY e.animeSlug
             ORDER BY COUNT(e) DESC, e.animeSlug ASC
            """)
    List<AnimeClickProjection> findTopAnimeClicks(Instant since, Pageable pageable);

    interface AnimeClickProjection {
        String getAnimeSlug();
        Long getClicks();
    }
}
