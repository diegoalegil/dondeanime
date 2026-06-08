package com.dondeanime.backend.subscription;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionNotificationMarker {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionNotificationMarker(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markNotifiedIfPending(Long subscriptionId, Instant notifiedAt) {
        return subscriptionRepository.markNotifiedIfPending(subscriptionId, notifiedAt) == 1;
    }
}
