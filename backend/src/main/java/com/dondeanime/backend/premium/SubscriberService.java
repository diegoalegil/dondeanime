package com.dondeanime.backend.premium;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

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

    @Transactional(readOnly = true)
    public Optional<String> findActiveStripeCustomerId(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank()) {
            return Optional.empty();
        }

        Instant now = Instant.now(clock);
        return subscriberRepository.findByEmail(normalizedEmail)
                .filter(subscriber -> isActive(subscriber, now))
                .map(Subscriber::getStripeCustomerId)
                .map(SubscriberService::normalizeStripeCustomerId)
                .filter(customerId -> !customerId.isBlank());
    }

    @Transactional
    public Subscriber upsertPremium(
            String email,
            String stripeCustomerId,
            String planTier,
            Instant subscribedAt,
            Instant expiresAt,
            Instant lastPaymentAt) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedCustomerId = normalizeStripeCustomerId(stripeCustomerId);
        if (normalizedEmail.isBlank() && normalizedCustomerId.isBlank()) {
            throw new IllegalArgumentException("email or stripeCustomerId is required");
        }

        Optional<Subscriber> existing = findExisting(normalizedEmail, normalizedCustomerId);
        if (existing.isEmpty() && normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("email is required for new subscriber");
        }
        Subscriber subscriber = existing.orElseGet(Subscriber::new);
        if (!normalizedEmail.isBlank()) {
            subscriber.setEmail(normalizedEmail);
        }
        if (!normalizedCustomerId.isBlank()) {
            subscriber.setStripeCustomerId(normalizedCustomerId);
        }
        subscriber.setPlanTier(planTier == null || planTier.isBlank() ? "PREMIUM" : planTier.trim().toUpperCase(Locale.ROOT));
        subscriber.setSubscribedAt(subscribedAt == null ? Instant.now(clock) : subscribedAt);
        subscriber.setExpiresAt(expiresAt);
        subscriber.setLastPaymentAt(lastPaymentAt);
        return subscriberRepository.save(subscriber);
    }

    @Transactional
    public boolean cancelByStripeCustomerId(String stripeCustomerId, Instant canceledAt) {
        String normalizedCustomerId = normalizeStripeCustomerId(stripeCustomerId);
        if (normalizedCustomerId.isBlank()) {
            return false;
        }
        return subscriberRepository.findByStripeCustomerId(normalizedCustomerId)
                .map(subscriber -> {
                    subscriber.setExpiresAt(canceledAt == null ? Instant.now(clock) : canceledAt);
                    subscriberRepository.save(subscriber);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public boolean recordPaymentSucceeded(String email, String stripeCustomerId, Instant paidAt) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedCustomerId = normalizeStripeCustomerId(stripeCustomerId);
        Optional<Subscriber> existing = findExisting(normalizedEmail, normalizedCustomerId);
        if (existing.isPresent()) {
            Subscriber subscriber = existing.get();
            subscriber.setLastPaymentAt(paidAt == null ? Instant.now(clock) : paidAt);
            subscriberRepository.save(subscriber);
            return true;
        }
        if (normalizedEmail.isBlank()) {
            return false;
        }
        upsertPremium(normalizedEmail, normalizedCustomerId, "PREMIUM", paidAt, null, paidAt);
        return true;
    }

    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private Optional<Subscriber> findExisting(String normalizedEmail, String normalizedCustomerId) {
        if (!normalizedCustomerId.isBlank()) {
            Optional<Subscriber> byCustomer = subscriberRepository.findByStripeCustomerId(normalizedCustomerId);
            if (byCustomer.isPresent()) {
                return byCustomer;
            }
        }
        if (!normalizedEmail.isBlank()) {
            return subscriberRepository.findByEmail(normalizedEmail);
        }
        return Optional.empty();
    }

    private static String normalizeStripeCustomerId(String stripeCustomerId) {
        return stripeCustomerId == null ? "" : stripeCustomerId.trim();
    }

    private static boolean isActive(Subscriber subscriber, Instant now) {
        return subscriber.getExpiresAt() == null || subscriber.getExpiresAt().isAfter(now);
    }
}
