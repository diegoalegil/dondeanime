package com.dondeanime.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class ApiKeyServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-25T12:00:00Z");
    private static final String RAW_KEY = "da_free_test";
    private static final String RAW_KEY_HASH = ApiKeyService.hashKey(RAW_KEY);

    private final ApiKeyRepository repository = org.mockito.Mockito.mock(ApiKeyRepository.class);
    private final ApiKeyEndpointUsageRepository endpointUsageRepository =
            org.mockito.Mockito.mock(ApiKeyEndpointUsageRepository.class);
    private final ApiKeyService service = new ApiKeyService(
            repository,
            endpointUsageRepository,
            Clock.fixed(NOW, ZoneOffset.UTC));

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
    void createPersistsOnlyHashAndPreviewNeverRawKey() {
        when(repository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiKeyDto dto = service.create(new ApiKeyCreateRequest("diego@example.com", "free"));

        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
        verify(repository).save(captor.capture());
        ApiKey saved = captor.getValue();
        assertThat(saved.getKeyHash()).isEqualTo(ApiKeyService.hashKey(dto.key()));
        assertThat(saved.getKeyHash()).hasSize(64).isNotEqualTo(dto.key());
        assertThat(saved.getKeyPreview()).isEqualTo(ApiKeyService.previewOf(dto.key()));
        assertThat(saved.getKeyPreview()).doesNotContain(dto.key());
    }

    @Test
    void findUsageLooksUpByHashOfRawKey() {
        ApiKey apiKey = apiKey(25, 1_000, NOW);
        when(repository.findByKeyHash(RAW_KEY_HASH)).thenReturn(Optional.of(apiKey));

        ApiKeyUsage usage = service.findUsage(" " + RAW_KEY + " ");

        assertThat(usage.remaining()).isEqualTo(975);
    }

    @Test
    void recordUsageIncrementsUsageAndUpdatesLastUsedAt() {
        ApiKey apiKey = apiKey(999, 1_000, Instant.parse("2026-05-01T00:00:00Z"));
        when(repository.findByKeyHashForUpdate(RAW_KEY_HASH)).thenReturn(Optional.of(apiKey));

        ApiKeyUsage usage = service.recordUsage(" da_free_test ");

        assertThat(apiKey.getMonthlyUsage()).isEqualTo(1_000);
        assertThat(apiKey.getLastUsedAt()).isEqualTo(NOW);
        assertThat(usage.remaining()).isZero();
    }

    @Test
    void recordUsageStoresEndpointUsage() {
        ApiKey apiKey = apiKey(1, 1_000, Instant.parse("2026-05-01T00:00:00Z"));
        when(repository.findByKeyHashForUpdate(RAW_KEY_HASH)).thenReturn(Optional.of(apiKey));
        when(endpointUsageRepository.findByApiKey_IdAndEndpoint(null, "/api/v1/anime"))
                .thenReturn(Optional.empty());

        ApiKeyUsage usage = service.recordUsage("da_free_test", "/api/v1/anime");

        ArgumentCaptor<ApiKeyEndpointUsage> captor = ArgumentCaptor.forClass(ApiKeyEndpointUsage.class);
        verify(endpointUsageRepository).save(captor.capture());
        ApiKeyEndpointUsage endpointUsage = captor.getValue();
        assertThat(usage.monthlyUsage()).isEqualTo(2);
        assertThat(endpointUsage.getApiKey()).isSameAs(apiKey);
        assertThat(endpointUsage.getEndpoint()).isEqualTo("/api/v1/anime");
        assertThat(endpointUsage.getMonthlyUsage()).isEqualTo(1);
        assertThat(endpointUsage.getLastUsedAt()).isEqualTo(NOW);
    }

    @Test
    void recordUsageResetsCounterWhenMonthChanges() {
        ApiKey apiKey = apiKey(900, 1_000, Instant.parse("2026-04-30T23:59:00Z"));
        when(repository.findByKeyHashForUpdate(RAW_KEY_HASH)).thenReturn(Optional.of(apiKey));

        ApiKeyUsage usage = service.recordUsage("da_free_test");

        assertThat(apiKey.getMonthlyUsage()).isEqualTo(1);
        assertThat(usage.remaining()).isEqualTo(999);
    }

    @Test
    void recordUsageResetsEndpointUsageWhenMonthChanges() {
        ApiKey apiKey = apiKey(900, 1_000, Instant.parse("2026-04-30T23:59:00Z"));
        ReflectionTestUtils.setField(apiKey, "id", 42L);
        when(repository.findByKeyHashForUpdate(RAW_KEY_HASH)).thenReturn(Optional.of(apiKey));
        when(endpointUsageRepository.findByApiKey_IdAndEndpoint(42L, "/api/v1/anime"))
                .thenReturn(Optional.empty());

        service.recordUsage("da_free_test", "/api/v1/anime");

        verify(endpointUsageRepository).resetMonthlyUsageByApiKeyId(42L);
    }

    @Test
    void recordUsageRejectsExhaustedQuota() {
        ApiKey apiKey = apiKey(1_000, 1_000, NOW);
        when(repository.findByKeyHashForUpdate(RAW_KEY_HASH)).thenReturn(Optional.of(apiKey));

        assertThatThrownBy(() -> service.recordUsage("da_free_test"))
                .isInstanceOf(ApiQuotaExceededException.class);
    }

    @Test
    void statsReturnsKeysAndTopEndpoints() {
        ApiKey apiKey = apiKey(25, 1_000, NOW);
        apiKey.setKeyPreview(ApiKeyService.previewOf("da_free_abcdefghijklmnopqrstuvwxyz"));
        when(repository.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(apiKey));
        when(endpointUsageRepository.findTopEndpoints(any()))
                .thenReturn(List.of(endpointTotal("/api/v1/anime", 25)));

        ApiKeyStatsDto stats = service.stats();

        assertThat(stats.keys()).hasSize(1);
        assertThat(stats.keys().getFirst().keyPreview()).isEqualTo("da_free_abcd...wxyz");
        assertThat(stats.keys().getFirst().remaining()).isEqualTo(975);
        assertThat(stats.topEndpoints()).containsExactly(new ApiEndpointUsageDto("/api/v1/anime", 25));
    }

    private static ApiKey apiKey(long monthlyUsage, long monthlyQuota, Instant lastUsedAt) {
        ApiKey apiKey = new ApiKey();
        apiKey.setKeyHash(RAW_KEY_HASH);
        apiKey.setKeyPreview(ApiKeyService.previewOf(RAW_KEY));
        apiKey.setOwnerEmail("diego@example.com");
        apiKey.setTier("FREE");
        apiKey.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        apiKey.setLastUsedAt(lastUsedAt);
        apiKey.setMonthlyUsage(monthlyUsage);
        apiKey.setMonthlyQuota(monthlyQuota);
        return apiKey;
    }

    private static ApiKeyEndpointUsageRepository.EndpointUsageTotal endpointTotal(
            String endpoint,
            long monthlyUsage) {
        return new ApiKeyEndpointUsageRepository.EndpointUsageTotal() {
            @Override
            public String getEndpoint() {
                return endpoint;
            }

            @Override
            public long getMonthlyUsage() {
                return monthlyUsage;
            }
        };
    }
}
