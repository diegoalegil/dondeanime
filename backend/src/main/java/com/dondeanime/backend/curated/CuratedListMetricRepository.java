package com.dondeanime.backend.curated;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CuratedListMetricRepository extends JpaRepository<CuratedListMetricEvent, Long> {

    long countByEventTypeAndOccurredAtAfter(CuratedListMetricType eventType, Instant since);

    @Query("""
            select event.listSlug as listSlug, count(event) as events
            from CuratedListMetricEvent event
            where event.eventType = :eventType
              and event.occurredAt > :since
            group by event.listSlug
            order by count(event) desc, event.listSlug asc
            """)
    List<ListMetricProjection> findTopLists(CuratedListMetricType eventType, Instant since, Pageable pageable);

    interface ListMetricProjection {
        String getListSlug();

        Long getEvents();
    }
}
