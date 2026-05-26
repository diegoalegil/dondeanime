package com.dondeanime.backend.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.dondeanime.backend.AbstractIntegrationTest;
import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;
import com.dondeanime.backend.anime.tmdb.TmdbClient;
import com.dondeanime.backend.anime.tmdb.TmdbCountryProviders;
import com.dondeanime.backend.anime.tmdb.TmdbProvider;
import com.dondeanime.backend.anime.tmdb.TmdbProvidersResponse;
import com.dondeanime.backend.subscription.AlertService;

@SpringBootTest
class ProviderSyncServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProviderSyncService service;

    @Autowired
    private AnimeRepository animeRepository;

    @Autowired
    private WatchProviderRepository providerRepository;

    @MockitoBean
    private TmdbClient tmdbClient;

    @MockitoBean
    private AlertService alertService;

    @BeforeEach
    void cleanDatabase() {
        providerRepository.deleteAll();
        animeRepository.deleteAll();
    }

    @Test
    void syncAllCanReplaceExistingProvidersWithoutDuplicateKey() {
        Anime anime = animeRepository.saveAndFlush(animeWithTmdbId());
        when(tmdbClient.getWatchProviders(101L))
                .thenReturn(providersResponse())
                .thenReturn(providersResponse());

        int firstRun = service.syncAll();
        int secondRun = service.syncAll();

        assertThat(firstRun).isEqualTo(1);
        assertThat(secondRun).isEqualTo(1);
        assertThat(providerRepository.findByAnimeIdOrderByCountryCodeAscProviderTypeAscProviderNameAsc(anime.getId()))
                .singleElement()
                .satisfies(provider -> {
                    assertThat(provider.getCountryCode()).isEqualTo("ES");
                    assertThat(provider.getProviderName()).isEqualTo("Crunchyroll");
                    assertThat(provider.getProviderType()).isEqualTo("FLATRATE");
                    assertThat(provider.getTmdbProviderId()).isEqualTo(283);
                });
    }

    private static Anime animeWithTmdbId() {
        Anime anime = new Anime();
        anime.setAnilistId(16498L);
        anime.setTmdbId(101L);
        anime.setSlug("attack-on-titan");
        anime.setTitleEnglish("Attack on Titan");
        anime.setTitleRomaji("Shingeki no Kyojin");
        anime.setDescription("Descripción AniList");
        anime.setFormat("TV");
        anime.setStatus("FINISHED");
        return anime;
    }

    private static TmdbProvidersResponse providersResponse() {
        TmdbProvider crunchyroll = new TmdbProvider(
                283,
                "Crunchyroll",
                "/crunchyroll.png",
                1);
        TmdbCountryProviders spain = new TmdbCountryProviders(
                "https://www.themoviedb.org/tv/101/watch",
                List.of(crunchyroll),
                null,
                null,
                null);
        return new TmdbProvidersResponse(101L, Map.of("ES", spain));
    }
}
