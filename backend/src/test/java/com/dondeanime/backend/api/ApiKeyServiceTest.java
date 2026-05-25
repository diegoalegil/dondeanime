package com.dondeanime.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class ApiKeyServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-25T12:00:00Z");

    private final ApiKeyRepository repository = org.mockito.Mockito.mock(ApiKeyRepository.class);
    private final ApiKeyService service = new ApiKeyService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void createGeneratesFreeKeyWithMonthlyQuota() {
        when(repository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiKeyDto dto = service.create(new ApiKeyCreateRequest(" Diego@Example.com ", "free"));

        assertThat(dto.key()).startsWith("da_free_");
        assertThat(dto.ownerEmail()).isEqualTo("diego@example.com");
        assertThat(dto.tier()).isEqualTo("FREE");
        assertThat(dto.monthlyQuota()).isEqualTo(1_000);
        assertThat(dto.monthlyUsage()).isZero();
        assertThat(dto.createdAt()).isEqualTo(NOW);
    }

    @Test
    void recordUsageIncrementsUsageAndUpdatesLastUsedAt() {
        ApiKey apiKey = apiKey(999, 1_000, Instant.parse("2026-05-01T00:00:00Z"));
        when(repository.findByKey("da_free_test")).thenReturn(Optional.of(apiKey));

        ApiKeyUsage usage = service.recordUsage(" da_free_test ");

        assertThat(apiKey.getMonthlyUsage()).isEqualTo(1_000);
        assertThat(apiKey.getLastUsedAt()).isEqualTo(NOW);
        assertThat(usage.remaining()).isZero();
    }

    @Test
    void recordUsageResetsCounterWhenMonthChanges() {
        ApiKey apiKey = apiKey(900, 1_000, Instant.parse("2026-04-30T23:59:00Z"));
        when(repository.findByKey("da_free_test")).thenReturn(Optional.of(apiKey));

        ApiKeyUsage usage = service.recordUsage("da_free_test");

        assertThat(apiKey.getMonthlyUsage()).isEqualTo(1);
        assertThat(usage.remaining()).isEqualTo(999);
    }

    @Test
    void recordUsageRejectsExhaustedQuota() {
        ApiKey apiKey = apiKey(1_000, 1_000, NOW);
        when(repository.findByKey("da_free_test")).thenReturn(Optional.of(apiKey));

        assertThatThrownBy(() -> service.recordUsage("da_free_test"))
                .isInstanceOf(ApiQuotaExceededException.class);
    }

    private static ApiKey apiKey(long monthlyUsage, long monthlyQuota, Instant lastUsedAt) {
        ApiKey apiKey = new ApiKey();
        apiKey.setKey("da_free_test");
        apiKey.setOwnerEmail("diego@example.com");
        apiKey.setTier("FREE");
        apiKey.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        apiKey.setLastUsedAt(lastUsedAt);
        apiKey.setMonthlyUsage(monthlyUsage);
        apiKey.setMonthlyQuota(monthlyQuota);
        return apiKey;
    }
}
