package com.dondeanime.backend.studio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StudioRepository extends JpaRepository<Studio, Long> {

    Optional<Studio> findByAnilistId(Long anilistId);

    Optional<Studio> findBySlug(String slug);

    @Query("""
            SELECT s AS studio, COUNT(DISTINCT a.id) AS animeCount
            FROM Anime a JOIN a.studios s
            WHERE s.animationStudio = true
            GROUP BY s
            ORDER BY COUNT(DISTINCT a.id) DESC, s.name ASC
            """)
    List<StudioAggregation> aggregateStudios();

    interface StudioAggregation {
        Studio getStudio();
        Long getAnimeCount();
    }
}
