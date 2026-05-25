package com.dondeanime.backend.push;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    Optional<PushSubscription> findByEndpoint(String endpoint);

    List<PushSubscription> findAllByOrderByCreatedAtDesc();

    List<PushSubscription> findByCountryIsoOrderByCreatedAtAsc(String countryIso);

    List<PushSubscription> findByCountryIsoAndUserEmailInOrderByCreatedAtAsc(
            String countryIso,
            Collection<String> userEmails);
}
