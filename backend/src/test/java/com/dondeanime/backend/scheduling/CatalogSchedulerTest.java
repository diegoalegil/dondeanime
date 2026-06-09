package com.dondeanime.backend.scheduling;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.dondeanime.backend.anime.AnimeDescriptionEnricher;
import com.dondeanime.backend.anime.AnimeMatchingService;
import com.dondeanime.backend.anime.AnimeSyncService;
import com.dondeanime.backend.anime.TrailerSyncService;
import com.dondeanime.backend.news.NewsIngestionService;
import com.dondeanime.backend.news.NewsProcessingResult;
import com.dondeanime.backend.news.NewsProcessingService;
import com.dondeanime.backend.provider.ProviderSyncService;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class CatalogSchedulerTest {

    @Test
    void syncAniListTriggersVercelDeployHookAfterSuccess() {
        AnimeSyncService syncService = mock(AnimeSyncService.class);
        AnimeMatchingService matchingService = mock(AnimeMatchingService.class);
        AnimeDescriptionEnricher descriptionEnricher = mock(AnimeDescriptionEnricher.class);
        ProviderSyncService providerSyncService = mock(ProviderSyncService.class);
        TrailerSyncService trailerSyncService = mock(TrailerSyncService.class);
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();

        when(syncService.syncPopular(AnimeSyncService.MAX_POPULAR_SYNC_COUNT)).thenReturn(500);
        server.expect(requestTo("https://vercel.example/deploy"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());

        CatalogScheduler scheduler = new CatalogScheduler(
                syncService,
                matchingService,
                descriptionEnricher,
                providerSyncService,
                trailerSyncService,
                restClientBuilder,
                new SimpleMeterRegistry(),
                mock(ApplicationEventPublisher.class),
                mock(NewsIngestionService.class),
                mock(NewsProcessingService.class),
                "https://vercel.example/deploy",
                false,
                false);

        scheduler.syncAniList();

        server.verify();
    }

    @Test
    void matchTmdbEnrichesSpanishDescriptionsAfterMatching() {
        AnimeSyncService syncService = mock(AnimeSyncService.class);
        AnimeMatchingService matchingService = mock(AnimeMatchingService.class);
        AnimeDescriptionEnricher descriptionEnricher = mock(AnimeDescriptionEnricher.class);
        ProviderSyncService providerSyncService = mock(ProviderSyncService.class);
        TrailerSyncService trailerSyncService = mock(TrailerSyncService.class);

        CatalogScheduler scheduler = new CatalogScheduler(
                syncService,
                matchingService,
                descriptionEnricher,
                providerSyncService,
                trailerSyncService,
                RestClient.builder(),
                new SimpleMeterRegistry(),
                mock(ApplicationEventPublisher.class),
                mock(NewsIngestionService.class),
                mock(NewsProcessingService.class),
                "",
                false,
                false);

        scheduler.matchTmdb();

        verify(matchingService).matchAll();
        verify(descriptionEnricher).enrichMissingSpanishDescriptions();
    }

    @Test
    void processNewsDoesNothingWhenFlagDisabled() {
        NewsProcessingService newsProcessingService = mock(NewsProcessingService.class);
        CatalogScheduler scheduler = scheduler(newsProcessingService, false);

        scheduler.processNews();

        verify(newsProcessingService, never()).processDrafts();
    }

    @Test
    void processNewsRunsWhenFlagEnabled() {
        NewsProcessingService newsProcessingService = mock(NewsProcessingService.class);
        when(newsProcessingService.processDrafts())
                .thenReturn(new NewsProcessingResult(true, 3, 3, 1, 2, 0));
        CatalogScheduler scheduler = scheduler(newsProcessingService, true);

        scheduler.processNews();

        verify(newsProcessingService).processDrafts();
    }

    private static CatalogScheduler scheduler(
            NewsProcessingService newsProcessingService,
            boolean newsProcessingEnabled) {
        return new CatalogScheduler(
                mock(AnimeSyncService.class),
                mock(AnimeMatchingService.class),
                mock(AnimeDescriptionEnricher.class),
                mock(ProviderSyncService.class),
                mock(TrailerSyncService.class),
                RestClient.builder(),
                new SimpleMeterRegistry(),
                mock(ApplicationEventPublisher.class),
                mock(NewsIngestionService.class),
                newsProcessingService,
                "",
                false,
                newsProcessingEnabled);
    }
}
