package com.dondeanime.backend.premium;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {

    Optional<Subscriber> findByEmail(String email);

    Optional<Subscriber> findByStripeCustomerId(String stripeCustomerId);

    @Query("""
            select subscriber.email from Subscriber subscriber
            where subscriber.email in :emails
              and (subscriber.expiresAt is null or subscriber.expiresAt > :now)
            """)
    Set<String> findActivePremiumEmails(Collection<String> emails, Instant now);
}
