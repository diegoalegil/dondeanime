package com.dondeanime.backend.curated;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CuratedListRepository extends JpaRepository<CuratedList, Long> {

    boolean existsBySlug(String slug);

    Optional<CuratedList> findBySlug(String slug);

    @EntityGraph(attributePaths = "items")
    @Query("SELECT curated FROM CuratedList curated WHERE curated.slug = :slug")
    Optional<CuratedList> findBySlugWithItems(@Param("slug") String slug);

    List<CuratedList> findAllByStatusOrderByTitleAsc(CuratedListStatus status);

    @EntityGraph(attributePaths = "items")
    List<CuratedList> findAllByStatusAndVisibilityOrderByTitleAsc(
            CuratedListStatus status,
            CuratedListVisibility visibility);
}
