package com.dondeanime.backend.push;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushSubscriptionCleanupService {

    static final int NOT_FOUND = 404;
    static final int GONE = 410;
    private static final int INACTIVE_DAYS = 60;

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final Clock clock;

    @Autowired
    public PushSubscriptionCleanupService(PushSubscriptionRepository pushSubscriptionRepository) {
        this(pushSubscriptionRepository, Clock.systemUTC());
    }

    PushSubscriptionCleanupService(PushSubscriptionRepository pushSubscriptionRepository, Clock clock) {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.clock = clock;
    }

    @Transactional
    public boolean recordDeliveryResult(PushSubscription subscription, int statusCode) {
        if (isBouncingStatus(statusCode)) {
            pushSubscriptionRepository.delete(subscription);
            return true;
        }

        subscription.recordDeliveryResult(statusCode, Instant.now(clock));
        pushSubscriptionRepository.save(subscription);
        return false;
    }

    @Transactional
    public int purgeInactiveSubscriptions() {
        Instant cutoff = Instant.now(clock).minus(INACTIVE_DAYS, ChronoUnit.DAYS);
        return pushSubscriptionRepository.deleteInactiveBefore(cutoff);
    }

    public static boolean isBouncingStatus(int statusCode) {
        return statusCode == NOT_FOUND || statusCode == GONE;
    }
}
