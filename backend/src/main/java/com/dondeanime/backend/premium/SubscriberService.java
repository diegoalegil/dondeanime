package com.dondeanime.backend.premium;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriberService {

    private final SubscriberRepository subscriberRepository;
    private final Clock clock;

    @Autowired
    public SubscriberService(SubscriberRepository subscriberRepository) {
        this(subscriberRepository, Clock.systemUTC());
    }

    SubscriberService(SubscriberRepository subscriberRepository, Clock clock) {
        this.subscriberRepository = subscriberRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public boolean isPremium(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank()) {
            return false;
        }

        Instant now = Instant.now(clock);
        return subscriberRepository.findByEmail(normalizedEmail)
                .filter(subscriber -> isActive(subscriber, now))
                .isPresent();
    }

    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isActive(Subscriber subscriber, Instant now) {
        return subscriber.getExpiresAt() == null || subscriber.getExpiresAt().isAfter(now);
    }
}
