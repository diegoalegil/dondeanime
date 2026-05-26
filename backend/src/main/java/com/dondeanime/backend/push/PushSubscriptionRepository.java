package com.dondeanime.backend.push;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    Optional<PushSubscription> findByEndpoint(String endpoint);

    List<PushSubscription> findAllByOrderByCreatedAtDesc();

    List<PushSubscription> findByCountryIsoOrderByCreatedAtAsc(String countryIso);

    List<PushSubscription> findByCountryIsoAndUserEmailInOrderByCreatedAtAsc(
            String countryIso,
            Collection<String> userEmails);

    @Modifying
    @Query("""
            delete from PushSubscription subscription
            where subscription.createdAt < :cutoff
              and (subscription.lastDeliveredAt is null or subscription.lastDeliveredAt < :cutoff)
              and (subscription.lastFailedAt is null or subscription.lastFailedAt < :cutoff)
            """)
    int deleteInactiveBefore(@Param("cutoff") Instant cutoff);
}
