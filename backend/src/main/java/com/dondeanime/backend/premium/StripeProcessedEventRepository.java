package com.dondeanime.backend.premium;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StripeProcessedEventRepository extends JpaRepository<StripeProcessedEvent, String> {
}
