package com.dondeanime.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.client.RestClient;

import com.dondeanime.backend.anime.AnimeDescriptionEnricher;
import com.dondeanime.backend.anime.AnimeMatchingService;
import com.dondeanime.backend.anime.AnimeSyncService;
import com.dondeanime.backend.provider.ProviderSyncService;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class CatalogSchedulerMetricsTest {

    @Test
    void syncAniListRecordsSuccessCounterAndDuration() {
        AnimeSyncService syncService = mock(AnimeSyncService.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CatalogScheduler scheduler = scheduler(syncService, mock(AnimeMatchingService.class),
                mock(ProviderSyncService.class), registry);
        when(syncService.syncPopular(100)).thenReturn(100);

        scheduler.syncAniList();

        assertThat(counter(registry, "dondeanime.scheduler.anilist.success.count")).isEqualTo(1.0);
        assertThat(timerCount(registry, "dondeanime.scheduler.anilist.duration")).isEqualTo(1);
    }

    @Test
    void matchTmdbRecordsErrorCounterAndDoesNotThrow() {
        AnimeMatchingService matchingService = mock(AnimeMatchingService.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        CatalogScheduler scheduler = scheduler(mock(AnimeSyncService.class), matchingService,
                mock(ProviderSyncService.class), registry, eventPublisher);
        when(matchingService.matchAll()).thenThrow(new IllegalStateException("boom"));

        assertThatCode(scheduler::matchTmdb).doesNotThrowAnyException();

        assertThat(counter(registry, "dondeanime.scheduler.match.error.count")).isEqualTo(1.0);
        assertThat(timerCount(registry, "dondeanime.scheduler.match.duration")).isEqualTo(1);

        ArgumentCaptor<SchedulerJobFailedEvent> eventCaptor = ArgumentCaptor.forClass(SchedulerJobFailedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().job()).isEqualTo("match");
        assertThat(eventCaptor.getValue().error()).hasMessage("boom");
    }

    @Test
    void syncProvidersRecordsSuccessBeforeOptionalDeployHook() {
        ProviderSyncService providerSyncService = mock(ProviderSyncService.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CatalogScheduler scheduler = scheduler(mock(AnimeSyncService.class), mock(AnimeMatchingService.class),
                providerSyncService, registry);
        when(providerSyncService.syncAll()).thenReturn(100);

        scheduler.syncProviders();

        assertThat(counter(registry, "dondeanime.scheduler.providers.success.count")).isEqualTo(1.0);
        assertThat(timerCount(registry, "dondeanime.scheduler.providers.duration")).isEqualTo(1);
    }

    private static CatalogScheduler scheduler(
            AnimeSyncService syncService,
            AnimeMatchingService matchingService,
            ProviderSyncService providerSyncService,
            SimpleMeterRegistry registry) {
        return new CatalogScheduler(
                syncService,
                matchingService,
                mock(AnimeDescriptionEnricher.class),
                providerSyncService,
                RestClient.builder(),
                registry,
                mock(ApplicationEventPublisher.class),
                "");
    }

    private static CatalogScheduler scheduler(
            AnimeSyncService syncService,
            AnimeMatchingService matchingService,
            ProviderSyncService providerSyncService,
            SimpleMeterRegistry registry,
            ApplicationEventPublisher eventPublisher) {
        return new CatalogScheduler(
                syncService,
                matchingService,
                mock(AnimeDescriptionEnricher.class),
                providerSyncService,
                RestClient.builder(),
                registry,
                eventPublisher,
                "");
    }

    private static double counter(SimpleMeterRegistry registry, String name) {
        return registry.find(name).counter().count();
    }

    private static long timerCount(SimpleMeterRegistry registry, String name) {
        return registry.find(name).timer().count();
    }
}
