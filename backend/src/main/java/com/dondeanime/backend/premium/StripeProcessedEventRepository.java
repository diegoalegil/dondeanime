package com.dondeanime.backend.premium;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StripeProcessedEventRepository extends JpaRepository<StripeProcessedEvent, String> {

    @Modifying
    @Query(value = """
            INSERT INTO stripe_processed_event (event_id, processed_at)
            VALUES (:eventId, :processedAt)
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int claimProcessing(@Param("eventId") String eventId, @Param("processedAt") Instant processedAt);
}
