package com.dondeanime.backend.api;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiKeyService {

    public static final String TIER_FREE = "FREE";
    public static final String TIER_PAID = "PAID";
    public static final long FREE_MONTHLY_QUOTA = 1_000;
    public static final long PAID_MONTHLY_QUOTA = 100_000;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ApiKeyRepository repository;
    private final Clock clock;

    public ApiKeyService(ApiKeyRepository repository) {
        this(repository, Clock.systemUTC());
    }

    ApiKeyService(ApiKeyRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public ApiKeyDto create(ApiKeyCreateRequest request) {
        String tier = normalizeTier(request.tier());
        ApiKey apiKey = new ApiKey();
        apiKey.setKey(generateUniqueKey(tier));
        apiKey.setOwnerEmail(normalizeEmail(request.ownerEmail()));
        apiKey.setTier(tier);
        apiKey.setCreatedAt(Instant.now(clock));
        apiKey.setMonthlyQuota(quotaForTier(tier));
        apiKey.setMonthlyUsage(0);
        return ApiKeyDto.from(repository.save(apiKey));
    }

    @Transactional(readOnly = true)
    public ApiKeyUsage findUsage(String rawKey) {
        ApiKey apiKey = repository.findByKey(normalizeKey(rawKey))
                .orElseThrow(ApiKeyNotFoundException::new);
        long remaining = Math.max(0, apiKey.getMonthlyQuota() - apiKey.getMonthlyUsage());
        return new ApiKeyUsage(apiKey.getTier(), apiKey.getMonthlyQuota(), apiKey.getMonthlyUsage(), remaining);
    }

    @Transactional
    public ApiKeyUsage recordUsage(String rawKey) {
        ApiKey apiKey = repository.findByKey(normalizeKey(rawKey))
                .orElseThrow(ApiKeyNotFoundException::new);
        Instant now = Instant.now(clock);
        resetMonthlyUsageIfNeeded(apiKey, now);
        if (apiKey.getMonthlyUsage() >= apiKey.getMonthlyQuota()) {
            throw new ApiQuotaExceededException();
        }
        apiKey.setMonthlyUsage(apiKey.getMonthlyUsage() + 1);
        apiKey.setLastUsedAt(now);
        long remaining = Math.max(0, apiKey.getMonthlyQuota() - apiKey.getMonthlyUsage());
        return new ApiKeyUsage(apiKey.getTier(), apiKey.getMonthlyQuota(), apiKey.getMonthlyUsage(), remaining);
    }

    static String normalizeTier(String tier) {
        String normalized = tier == null ? "" : tier.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case TIER_FREE, TIER_PAID -> normalized;
            default -> throw new IllegalArgumentException("tier debe ser FREE o PAID");
        };
    }

    static long quotaForTier(String tier) {
        return TIER_PAID.equals(tier) ? PAID_MONTHLY_QUOTA : FREE_MONTHLY_QUOTA;
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeKey(String rawKey) {
        return rawKey == null ? "" : rawKey.trim();
    }

    private String generateUniqueKey(String tier) {
        String prefix = "da_" + tier.toLowerCase(Locale.ROOT) + "_";
        String key;
        do {
            byte[] bytes = new byte[24];
            RANDOM.nextBytes(bytes);
            key = prefix + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (repository.existsByKey(key));
        return key;
    }

    private void resetMonthlyUsageIfNeeded(ApiKey apiKey, Instant now) {
        if (apiKey.getLastUsedAt() == null) {
            return;
        }
        YearMonth currentMonth = YearMonth.from(now.atZone(ZoneOffset.UTC));
        YearMonth lastUsedMonth = YearMonth.from(apiKey.getLastUsedAt().atZone(ZoneOffset.UTC));
        if (!currentMonth.equals(lastUsedMonth)) {
            apiKey.setMonthlyUsage(0);
        }
    }
}
