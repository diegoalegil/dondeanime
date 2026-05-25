package com.dondeanime.backend.provider;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AvailabilityChangeEventRepository extends JpaRepository<AvailabilityChangeEvent, Long> {

    @Query("""
            SELECT e.animeSlug AS animeSlug, COUNT(e) AS changes
              FROM AvailabilityChangeEvent e
             WHERE e.changedAt >= :since
             GROUP BY e.animeSlug
             ORDER BY COUNT(e) DESC, e.animeSlug ASC
            """)
    List<AnimeAvailabilityChangeProjection> findTopAnimeChanges(Instant since, Pageable pageable);

    interface AnimeAvailabilityChangeProjection {
        String getAnimeSlug();
        Long getChanges();
    }
}
