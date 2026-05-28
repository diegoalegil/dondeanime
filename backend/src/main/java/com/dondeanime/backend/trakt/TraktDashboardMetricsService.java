package com.dondeanime.backend.trakt;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

@Service
public class TraktDashboardMetricsService {

    private static final String PROVIDER = "trakt";

    private final ExternalAccountRepository externalAccountRepository;
    private final TraktSyncEventRepository syncEventRepository;
    private final Clock clock;

    public TraktDashboardMetricsService(
            ExternalAccountRepository externalAccountRepository,
            TraktSyncEventRepository syncEventRepository,
            Clock clock) {
        this.externalAccountRepository = externalAccountRepository;
        this.syncEventRepository = syncEventRepository;
        this.clock = clock;
    }

    public TraktDashboardMetricsDto metrics() {
        Instant now = Instant.now(clock);
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);

        return new TraktDashboardMetricsDto(
                externalAccountRepository.countByProvider(PROVIDER),
                syncEventRepository.countByProviderAndSyncedAtAfter(PROVIDER, sevenDaysAgo),
                syncEventRepository.countByProviderAndSyncedAtAfter(PROVIDER, thirtyDaysAgo),
                syncEventRepository.sumUnmatchedSince(PROVIDER, thirtyDaysAgo));
    }
}
