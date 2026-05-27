package com.dondeanime.backend.trakt;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TraktSyncEventRepository extends JpaRepository<TraktSyncEvent, Long> {

    Long countByProviderAndSyncedAtAfter(String provider, Instant since);

    @Query("""
            SELECT COALESCE(SUM(event.unmatchedCount), 0)
            FROM TraktSyncEvent event
            WHERE event.provider = :provider
            AND event.syncedAt >= :since
            """)
    Long sumUnmatchedSince(@Param("provider") String provider, @Param("since") Instant since);
}
