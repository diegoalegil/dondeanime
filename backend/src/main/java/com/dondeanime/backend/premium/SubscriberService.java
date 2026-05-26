package com.dondeanime.backend.premium;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriberService {

    private static final Set<String> DASHBOARD_ACCESS_TIERS = Set.of("PATRON", "PATRON_PLUS", "PATRON_HIGH");

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

    @Transactional(readOnly = true)
    public Set<String> findActivePremiumEmails(Collection<String> emails) {
        if (emails == null || emails.isEmpty()) {
            return Set.of();
        }

        Set<String> normalizedEmails = emails.stream()
                .map(SubscriberService::normalizeEmail)
                .filter(email -> !email.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (normalizedEmails.isEmpty()) {
            return Set.of();
        }

        return subscriberRepository.findActivePremiumEmails(normalizedEmails, Instant.now(clock)).stream()
                .map(SubscriberService::normalizeEmail)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Transactional(readOnly = true)
    public boolean canAccessAdminDashboard(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank()) {
            return false;
        }

        Instant now = Instant.now(clock);
        return subscriberRepository.findByEmail(normalizedEmail)
                .filter(subscriber -> isActive(subscriber, now))
                .map(Subscriber::getPlanTier)
                .map(SubscriberService::normalizeTier)
                .filter(DASHBOARD_ACCESS_TIERS::contains)
                .isPresent();
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
        subscriber.setCancellationEmailSentAt(null);
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
                    subscriber.setCancellationEmailSentAt(null);
                    subscriberRepository.save(subscriber);
                    return true;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Optional<String> findEmailByStripeCustomerId(String stripeCustomerId) {
        String normalizedCustomerId = normalizeStripeCustomerId(stripeCustomerId);
        if (normalizedCustomerId.isBlank()) {
            return Optional.empty();
        }
        return subscriberRepository.findByStripeCustomerId(normalizedCustomerId)
                .map(Subscriber::getEmail)
                .map(SubscriberService::normalizeEmail)
                .filter(email -> !email.isBlank());
    }

    @Transactional(readOnly = true)
    public List<Subscriber> findDueCancellationEmails(Instant cutoff) {
        return subscriberRepository.findDueCancellationEmails(cutoff);
    }

    @Transactional
    public boolean markCancellationEmailSent(Long subscriberId, Instant sentAt) {
        if (subscriberId == null) {
            return false;
        }
        return subscriberRepository.findById(subscriberId)
                .map(subscriber -> {
                    subscriber.setCancellationEmailSentAt(sentAt == null ? Instant.now(clock) : sentAt);
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

    private static String normalizeTier(String planTier) {
        return planTier == null ? "" : planTier.trim().toUpperCase(Locale.ROOT);
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
