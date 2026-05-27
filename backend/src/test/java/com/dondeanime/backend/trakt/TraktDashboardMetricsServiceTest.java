package com.dondeanime.backend.trakt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class TraktDashboardMetricsServiceTest {

    private final ExternalAccountRepository externalAccountRepository = mock(ExternalAccountRepository.class);
    private final TraktSyncEventRepository syncEventRepository = mock(TraktSyncEventRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-27T12:00:00Z"), ZoneOffset.UTC);
    private final TraktDashboardMetricsService service = new TraktDashboardMetricsService(
            externalAccountRepository,
            syncEventRepository,
            clock);

    @Test
    void returnsAnonymousAggregatedMetricsOnly() {
        Instant sevenDaysAgo = Instant.parse("2026-05-20T12:00:00Z");
        Instant thirtyDaysAgo = Instant.parse("2026-04-27T12:00:00Z");

        when(externalAccountRepository.countByProvider("trakt")).thenReturn(3L);
        when(syncEventRepository.countByProviderAndSyncedAtAfter("trakt", sevenDaysAgo)).thenReturn(2L);
        when(syncEventRepository.countByProviderAndSyncedAtAfter("trakt", thirtyDaysAgo)).thenReturn(8L);
        when(syncEventRepository.sumUnmatchedSince(eq("trakt"), eq(thirtyDaysAgo))).thenReturn(5L);

        TraktDashboardMetricsDto metrics = service.metrics();

        assertThat(metrics.connectedAccounts()).isEqualTo(3L);
        assertThat(metrics.syncsLast7Days()).isEqualTo(2L);
        assertThat(metrics.syncsLast30Days()).isEqualTo(8L);
        assertThat(metrics.failedMatchesLast30Days()).isEqualTo(5L);
    }
}
