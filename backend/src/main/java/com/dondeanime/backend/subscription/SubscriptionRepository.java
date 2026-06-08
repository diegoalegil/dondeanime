package com.dondeanime.backend.subscription;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUser_IdAndAnime_IdAndCountryCode(
            Long userId,
            Long animeId,
            String countryCode);

    long countByNotifiedAtAfter(Instant notifiedAt);

    long countByUser_IdAndNotifiedAtIsNull(Long userId);

    @Query("""
            SELECT COUNT(s) FROM Subscription s
            JOIN s.user u
            JOIN s.anime a
            WHERE a.id = :animeId
              AND s.countryCode = :countryCode
              AND s.notifiedAt IS NULL
              AND u.confirmedAt IS NOT NULL
              AND u.unsubscribedAt IS NULL
            """)
    long countPendingAlerts(Long animeId, String countryCode);

    @Query("""
            SELECT s FROM Subscription s
            JOIN FETCH s.user u
            JOIN FETCH s.anime a
            WHERE a.id = :animeId
              AND s.countryCode = :countryCode
              AND s.notifiedAt IS NULL
              AND u.confirmedAt IS NOT NULL
              AND u.unsubscribedAt IS NULL
            ORDER BY s.createdAt ASC
            """)
    List<Subscription> findPendingAlerts(Long animeId, String countryCode);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Subscription s
            SET s.notifiedAt = :notifiedAt
            WHERE s.id = :subscriptionId
              AND s.notifiedAt IS NULL
            """)
    int markNotifiedIfPending(
            @Param("subscriptionId") Long subscriptionId,
            @Param("notifiedAt") Instant notifiedAt);
}
