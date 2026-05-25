package com.dondeanime.backend.premium;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {

    Optional<Subscriber> findByEmail(String email);

    Optional<Subscriber> findByStripeCustomerId(String stripeCustomerId);
}
