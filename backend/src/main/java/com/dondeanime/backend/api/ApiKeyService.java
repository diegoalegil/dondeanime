package com.dondeanime.backend.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    private final ApiKeyEndpointUsageRepository endpointUsageRepository;
    private final Clock clock;

    @Autowired
    public ApiKeyService(
            ApiKeyRepository repository,
            ApiKeyEndpointUsageRepository endpointUsageRepository) {
        this(repository, endpointUsageRepository, Clock.systemUTC());
    }

    ApiKeyService(
            ApiKeyRepository repository,
            ApiKeyEndpointUsageRepository endpointUsageRepository,
            Clock clock) {
        this.repository = repository;
        this.endpointUsageRepository = endpointUsageRepository;
        this.clock = clock;
    }

    @Transactional
    public ApiKeyDto create(ApiKeyCreateRequest request) {
        String tier = normalizeTier(request.tier());
        String rawKey = generateUniqueKey(tier);
        ApiKey apiKey = new ApiKey();
        apiKey.setKeyHash(hashKey(rawKey));
        apiKey.setKeyPreview(previewOf(rawKey));
        apiKey.setOwnerEmail(normalizeEmail(request.ownerEmail()));
        apiKey.setTier(tier);
        apiKey.setCreatedAt(Instant.now(clock));
        apiKey.setMonthlyQuota(quotaForTier(tier));
        apiKey.setMonthlyUsage(0);
        return ApiKeyDto.from(repository.save(apiKey), rawKey);
    }

    @Transactional(readOnly = true)
    public ApiKeyUsage findUsage(String rawKey) {
        ApiKey apiKey = repository.findByKeyHash(hashKey(normalizeKey(rawKey)))
                .orElseThrow(ApiKeyNotFoundException::new);
        long remaining = Math.max(0, apiKey.getMonthlyQuota() - apiKey.getMonthlyUsage());
        return new ApiKeyUsage(apiKey.getTier(), apiKey.getMonthlyQuota(), apiKey.getMonthlyUsage(), remaining);
    }

    @Transactional
    public ApiKeyUsage recordUsage(String rawKey) {
        return recordUsage(rawKey, null);
    }

    @Transactional
    public ApiKeyUsage recordUsage(String rawKey, String endpoint) {
        ApiKey apiKey = repository.findByKeyHashForUpdate(hashKey(normalizeKey(rawKey)))
                .orElseThrow(ApiKeyNotFoundException::new);
        Instant now = Instant.now(clock);
        boolean reset = resetMonthlyUsageIfNeeded(apiKey, now);
        if (apiKey.getMonthlyUsage() >= apiKey.getMonthlyQuota()) {
            throw new ApiQuotaExceededException();
        }
        apiKey.setMonthlyUsage(apiKey.getMonthlyUsage() + 1);
        apiKey.setLastUsedAt(now);
        recordEndpointUsage(apiKey, endpoint, now, reset);
        long remaining = Math.max(0, apiKey.getMonthlyQuota() - apiKey.getMonthlyUsage());
        return new ApiKeyUsage(apiKey.getTier(), apiKey.getMonthlyQuota(), apiKey.getMonthlyUsage(), remaining);
    }

    @Transactional(readOnly = true)
    public ApiKeyStatsDto stats() {
        var keys = repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(ApiKeyStatsRowDto::from)
                .toList();
        var topEndpoints = endpointUsageRepository.findTopEndpoints(PageRequest.of(0, 10)).stream()
                .map(ApiEndpointUsageDto::from)
                .toList();
        return new ApiKeyStatsDto(keys, topEndpoints);
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

    static String hashKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawKey.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular hash de API key", e);
        }
    }

    static String previewOf(String rawKey) {
        if (rawKey == null || rawKey.length() <= 16) {
            return rawKey;
        }
        return rawKey.substring(0, 12) + "..." + rawKey.substring(rawKey.length() - 4);
    }

    private String generateUniqueKey(String tier) {
        String prefix = "da_" + tier.toLowerCase(Locale.ROOT) + "_";
        String key;
        do {
            byte[] bytes = new byte[24];
            RANDOM.nextBytes(bytes);
            key = prefix + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (repository.existsByKeyHash(hashKey(key)));
        return key;
    }

    private void recordEndpointUsage(ApiKey apiKey, String endpoint, Instant now, boolean reset) {
        String normalizedEndpoint = normalizeEndpoint(endpoint);
        if (normalizedEndpoint.isBlank()) {
            return;
        }
        if (reset && apiKey.getId() != null) {
            endpointUsageRepository.resetMonthlyUsageByApiKeyId(apiKey.getId());
        }
        ApiKeyEndpointUsage usage = endpointUsageRepository
                .findByApiKey_IdAndEndpoint(apiKey.getId(), normalizedEndpoint)
                .orElseGet(() -> {
                    ApiKeyEndpointUsage newUsage = new ApiKeyEndpointUsage();
                    newUsage.setApiKey(apiKey);
                    newUsage.setEndpoint(normalizedEndpoint);
                    newUsage.setMonthlyUsage(0);
                    return newUsage;
                });
        usage.setMonthlyUsage(usage.getMonthlyUsage() + 1);
        usage.setLastUsedAt(now);
        endpointUsageRepository.save(usage);
    }

    private static String normalizeEndpoint(String endpoint) {
        if (endpoint == null) {
            return "";
        }
        String normalized = endpoint.trim();
        if (normalized.length() > 255) {
            return normalized.substring(0, 255);
        }
        return normalized;
    }

    private boolean resetMonthlyUsageIfNeeded(ApiKey apiKey, Instant now) {
        if (apiKey.getLastUsedAt() == null) {
            return false;
        }
        YearMonth currentMonth = YearMonth.from(now.atZone(ZoneOffset.UTC));
        YearMonth lastUsedMonth = YearMonth.from(apiKey.getLastUsedAt().atZone(ZoneOffset.UTC));
        if (!currentMonth.equals(lastUsedMonth)) {
            apiKey.setMonthlyUsage(0);
            return true;
        }
        return false;
    }
}
