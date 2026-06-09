package com.dondeanime.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.client.RestClient;

import com.dondeanime.backend.anime.AnimeDescriptionEnricher;
import com.dondeanime.backend.anime.AnimeMatchingService;
import com.dondeanime.backend.anime.AnimeSyncService;
import com.dondeanime.backend.anime.TrailerSyncService;
import com.dondeanime.backend.news.NewsIngestionResult;
import com.dondeanime.backend.news.NewsIngestionService;
import com.dondeanime.backend.news.NewsProcessingService;
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

    @Test
    void ingestNewsRecordsSuccessWhenNoSaveErrors() {
        NewsIngestionService newsService = mock(NewsIngestionService.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        when(newsService.ingestAll()).thenReturn(new NewsIngestionResult(1, 2, 0, 0, List.of()));

        newsScheduler(newsService, registry).ingestNews();

        assertThat(counter(registry, "dondeanime.scheduler.news.success.count")).isEqualTo(1.0);
        assertThat(timerCount(registry, "dondeanime.scheduler.news.duration")).isEqualTo(1);
    }

    @Test
    void ingestNewsRecordsErrorWhenSaveErrors() {
        // Un fallo real de guardado (itemsErrored > 0) NO debe verse como éxito.
        NewsIngestionService newsService = mock(NewsIngestionService.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        when(newsService.ingestAll()).thenReturn(new NewsIngestionResult(1, 0, 0, 1, List.of()));

        newsScheduler(newsService, registry).ingestNews();

        assertThat(counter(registry, "dondeanime.scheduler.news.error.count")).isEqualTo(1.0);
        assertThat(timerCount(registry, "dondeanime.scheduler.news.duration")).isEqualTo(1);
    }

    private static CatalogScheduler newsScheduler(NewsIngestionService newsService, SimpleMeterRegistry registry) {
        return new CatalogScheduler(
                mock(AnimeSyncService.class),
                mock(AnimeMatchingService.class),
                mock(AnimeDescriptionEnricher.class),
                mock(ProviderSyncService.class),
                mock(TrailerSyncService.class),
                RestClient.builder(),
                registry,
                mock(ApplicationEventPublisher.class),
                newsService,
                mock(NewsProcessingService.class),
                "",
                true,
                false);
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
                mock(TrailerSyncService.class),
                RestClient.builder(),
                registry,
                mock(ApplicationEventPublisher.class),
                mock(NewsIngestionService.class),
                mock(NewsProcessingService.class),
                "",
                false,
                false);
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
                mock(TrailerSyncService.class),
                RestClient.builder(),
                registry,
                eventPublisher,
                mock(NewsIngestionService.class),
                mock(NewsProcessingService.class),
                "",
                false,
                false);
    }

    private static double counter(SimpleMeterRegistry registry, String name) {
        return registry.find(name).counter().count();
    }

    private static long timerCount(SimpleMeterRegistry registry, String name) {
        return registry.find(name).timer().count();
    }
}
