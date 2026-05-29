package com.dondeanime.backend.curated;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CuratedListItemRepository extends JpaRepository<CuratedListItem, Long> {

    List<CuratedListItem> findByCuratedListSlugOrderByPositionAsc(String slug);
}
