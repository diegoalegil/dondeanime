package com.dondeanime.backend.scheduling;

import static org.mockito.Mockito.mock;
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
import com.dondeanime.backend.provider.ProviderSyncService;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class CatalogSchedulerTest {

    @Test
    void syncAniListTriggersVercelDeployHookAfterSuccess() {
        AnimeSyncService syncService = mock(AnimeSyncService.class);
        AnimeMatchingService matchingService = mock(AnimeMatchingService.class);
        AnimeDescriptionEnricher descriptionEnricher = mock(AnimeDescriptionEnricher.class);
        ProviderSyncService providerSyncService = mock(ProviderSyncService.class);
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();

        when(syncService.syncPopular(100)).thenReturn(500);
        server.expect(requestTo("https://vercel.example/deploy"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());

        CatalogScheduler scheduler = new CatalogScheduler(
                syncService,
                matchingService,
                descriptionEnricher,
                providerSyncService,
                restClientBuilder,
                new SimpleMeterRegistry(),
                mock(ApplicationEventPublisher.class),
                "https://vercel.example/deploy");

        scheduler.syncAniList();

        server.verify();
    }

    @Test
    void matchTmdbEnrichesSpanishDescriptionsAfterMatching() {
        AnimeSyncService syncService = mock(AnimeSyncService.class);
        AnimeMatchingService matchingService = mock(AnimeMatchingService.class);
        AnimeDescriptionEnricher descriptionEnricher = mock(AnimeDescriptionEnricher.class);
        ProviderSyncService providerSyncService = mock(ProviderSyncService.class);

        CatalogScheduler scheduler = new CatalogScheduler(
                syncService,
                matchingService,
                descriptionEnricher,
                providerSyncService,
                RestClient.builder(),
                new SimpleMeterRegistry(),
                mock(ApplicationEventPublisher.class),
                "");

        scheduler.matchTmdb();

        verify(matchingService).matchAll();
        verify(descriptionEnricher).enrichMissingSpanishDescriptions();
    }
}
