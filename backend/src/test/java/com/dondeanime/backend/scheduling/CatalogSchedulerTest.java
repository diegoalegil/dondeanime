package com.dondeanime.backend.scheduling;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;

import com.dondeanime.backend.anime.AnimeDescriptionEnricher;
import com.dondeanime.backend.anime.AnimeMatchingService;
import com.dondeanime.backend.anime.AnimeSyncService;
import com.dondeanime.backend.anime.TrailerSyncService;
import com.dondeanime.backend.provider.ProviderSyncService;

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
        TrailerSyncService trailerSyncService = mock(TrailerSyncService.class);

        CatalogScheduler scheduler = new CatalogScheduler(
                syncService,
                matchingService,
                descriptionEnricher,
                providerSyncService,
                trailerSyncService,
                RestClient.builder(),
                "");

        scheduler.matchTmdb();

        verify(matchingService).matchAll();
        verify(descriptionEnricher).enrichMissingSpanishDescriptions();
    }
}
