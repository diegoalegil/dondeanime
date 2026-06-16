package com.dondeanime.backend.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;
import io.github.diegoalegil.tsunagi.tmdb.TmdbClient;
import io.github.diegoalegil.tsunagi.tmdb.TmdbCountryProviders;
import io.github.diegoalegil.tsunagi.tmdb.TmdbProvider;
import io.github.diegoalegil.tsunagi.tmdb.TmdbProvidersResponse;
import com.dondeanime.backend.subscription.AlertService;

class ProviderSyncServiceTest {

    @Test
    void publishesProviderAddedEventWhenNewProviderHasPendingAlerts() {
        Anime anime = anime();
        TmdbClient client = mock(TmdbClient.class);
        AnimeRepository animeRepository = mock(AnimeRepository.class);
        WatchProviderRepository providerRepository = mock(WatchProviderRepository.class);
        AlertService alertService = mock(AlertService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        AvailabilityChangeService availabilityChangeService = mock(AvailabilityChangeService.class);
        ProviderSyncService service = new ProviderSyncService(
                client,
                animeRepository,
                providerRepository,
                alertService,
                eventPublisher,
                availabilityChangeService,
                transactionManager());

        when(animeRepository.findAll()).thenReturn(List.of(anime));
        when(providerRepository.findByAnimeIdOrderByCountryCodeAscProviderTypeAscProviderNameAsc(1L))
                .thenReturn(List.of());
        when(client.getWatchProviders(10L)).thenReturn(providerResponse());
        when(alertService.hasPendingAlerts(1L, "ES")).thenReturn(true);

        int processed = service.syncAll();

        assertThat(processed).isEqualTo(1);
        ArgumentCaptor<ProviderAddedEvent> captor = ArgumentCaptor.forClass(ProviderAddedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ProviderAddedEvent event = captor.getValue();
        assertThat(event.anime()).isSameAs(anime);
        assertThat(event.countryCode()).isEqualTo("ES");
        assertThat(event.providers()).extracting(WatchProvider::getProviderName).containsExactly("Crunchyroll");
    }

    @Test
    void skipsProviderAddedEventWhenThereAreNoPendingAlerts() {
        Anime anime = anime();
        TmdbClient client = mock(TmdbClient.class);
        AnimeRepository animeRepository = mock(AnimeRepository.class);
        WatchProviderRepository providerRepository = mock(WatchProviderRepository.class);
        AlertService alertService = mock(AlertService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        AvailabilityChangeService availabilityChangeService = mock(AvailabilityChangeService.class);
        ProviderSyncService service = new ProviderSyncService(
                client,
                animeRepository,
                providerRepository,
                alertService,
                eventPublisher,
                availabilityChangeService,
                transactionManager());

        when(animeRepository.findAll()).thenReturn(List.of(anime));
        when(providerRepository.findByAnimeIdOrderByCountryCodeAscProviderTypeAscProviderNameAsc(1L))
                .thenReturn(List.of());
        when(client.getWatchProviders(10L)).thenReturn(providerResponse());
        when(alertService.hasPendingAlerts(1L, "ES")).thenReturn(false);

        service.syncAll();

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void skipsMovieFormatAnime() {
        // getWatchProviders pega a /tv/{id}/watch/providers: con un tmdbId de
        // película traería las plataformas de una serie sin relación.
        Anime anime = anime();
        anime.setFormat("MOVIE");
        TmdbClient client = mock(TmdbClient.class);
        AnimeRepository animeRepository = mock(AnimeRepository.class);
        WatchProviderRepository providerRepository = mock(WatchProviderRepository.class);
        ProviderSyncService service = new ProviderSyncService(
                client,
                animeRepository,
                providerRepository,
                mock(AlertService.class),
                mock(ApplicationEventPublisher.class),
                mock(AvailabilityChangeService.class),
                transactionManager());

        when(animeRepository.findAll()).thenReturn(List.of(anime));

        int processed = service.syncAll();

        assertThat(processed).isZero();
        verify(client, never()).getWatchProviders(any());
        verify(providerRepository, never()).save(any());
    }

    @Test
    void persistsRentAndBuyDedupedByBestOffer() {
        Anime anime = anime();
        TmdbClient client = mock(TmdbClient.class);
        AnimeRepository animeRepository = mock(AnimeRepository.class);
        WatchProviderRepository providerRepository = mock(WatchProviderRepository.class);
        ProviderSyncService service = new ProviderSyncService(
                client,
                animeRepository,
                providerRepository,
                mock(AlertService.class),
                mock(ApplicationEventPublisher.class),
                mock(AvailabilityChangeService.class),
                transactionManager());

        when(animeRepository.findAll()).thenReturn(List.of(anime));
        when(providerRepository.findByAnimeIdOrderByCountryCodeAscProviderTypeAscProviderNameAsc(1L))
                .thenReturn(List.of());
        when(client.getWatchProviders(10L)).thenReturn(multiOfferResponse());

        service.syncAll();

        // Prime Video aparece en flatrate y en rent -> se queda FLATRATE.
        // Apple TV aparece en rent y en buy -> se queda RENT. Sin duplicados
        // (la unique constraint es por anime+country+provider_name).
        ArgumentCaptor<WatchProvider> captor = ArgumentCaptor.forClass(WatchProvider.class);
        verify(providerRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(WatchProvider::getProviderName, WatchProvider::getProviderType)
                .containsExactlyInAnyOrder(
                        tuple("Prime Video", "FLATRATE"),
                        tuple("Apple TV", "RENT"));
    }

    private static Anime anime() {
        Anime anime = new Anime();
        anime.setId(1L);
        anime.setSlug("attack-on-titan");
        anime.setTitleEnglish("Attack on Titan");
        anime.setTmdbId(10L);
        return anime;
    }

    private static TmdbProvidersResponse providerResponse() {
        TmdbProvider provider = new TmdbProvider(283, "Crunchyroll", "/logo.png", 1);
        TmdbCountryProviders countryProviders = new TmdbCountryProviders(
                "https://example.com",
                List.of(provider),
                null,
                null,
                null);
        return new TmdbProvidersResponse(10L, Map.of("ES", countryProviders));
    }

    private static TmdbProvidersResponse multiOfferResponse() {
        TmdbProvider prime = new TmdbProvider(9, "Prime Video", "/prime.png", 1);
        TmdbProvider apple = new TmdbProvider(2, "Apple TV", "/apple.png", 2);
        TmdbCountryProviders spain = new TmdbCountryProviders(
                "https://example.com",
                List.of(prime),         // flatrate
                null,                   // free
                List.of(apple, prime),  // rent: prime ya está en flatrate -> se ignora
                List.of(apple));        // buy: apple ya está en rent -> se ignora
        return new TmdbProvidersResponse(10L, Map.of("ES", spain));
    }

    private static PlatformTransactionManager transactionManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) throws TransactionException {
            }

            @Override
            public void rollback(TransactionStatus status) throws TransactionException {
            }
        };
    }
}
