package com.dondeanime.backend.anime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.dondeanime.backend.anime.tmdb.TmdbClient;
import com.dondeanime.backend.anime.tmdb.TmdbSearchResponse;
import com.dondeanime.backend.anime.tmdb.TmdbSearchResult;

/**
 * Tests unitarios de la heurística de matching contra TMDb.
 *
 * Mockeamos TmdbClient para fabricar respuestas reproducibles y
 * AnimeRepository para capturar lo que se guarda. Sin Spring, sin BD.
 *
 * Ojo: matchAll() hace Thread.sleep(300) entre animes para respetar
 * rate limit. Cada test usa un solo anime para no penalizar demasiado.
 */
class AnimeMatchingServiceTest {

    @Test
    void preferJapaneseWithMatchingYearOverMorePopularWrongYear() {
        // Caso real que dio guerra: "My Hero Academia 2016" vs "MHA: Vigilantes 2025".
        // El segundo tiene más popularidad pero año equivocado. Debe ganar el primero.
        Anime anime = anime(123L, "My Hero Academia", 2016);

        TmdbSearchResult correctYear = result(65930L, "JP", "2016-04-03", 23.8);
        TmdbSearchResult wrongYearMorePopular = result(280110L, "JP", "2025-04-07", 26.2);

        AnimeRepository repo = mock(AnimeRepository.class);
        when(repo.findAll()).thenReturn(List.of(anime));
        TmdbClient client = mock(TmdbClient.class);
        when(client.searchTv(any())).thenReturn(
                new TmdbSearchResponse(1, List.of(wrongYearMorePopular, correctYear), 2, 1));

        new AnimeMatchingService(client, repo).matchAll();

        ArgumentCaptor<Anime> captor = ArgumentCaptor.forClass(Anime.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getTmdbId()).isEqualTo(65930L);
    }

    @Test
    void fallbackToJapaneseAnyYearIfNoneMatchesYear() {
        Anime anime = anime(1L, "Some Anime", 2010);

        TmdbSearchResult jpFar = result(1L, "JP", "2020-01-01", 10.0);
        TmdbSearchResult jpFarMorePopular = result(2L, "JP", "2022-01-01", 50.0);

        AnimeRepository repo = mock(AnimeRepository.class);
        when(repo.findAll()).thenReturn(List.of(anime));
        TmdbClient client = mock(TmdbClient.class);
        when(client.searchTv(any())).thenReturn(
                new TmdbSearchResponse(1, List.of(jpFar, jpFarMorePopular), 2, 1));

        new AnimeMatchingService(client, repo).matchAll();

        ArgumentCaptor<Anime> captor = ArgumentCaptor.forClass(Anime.class);
        verify(repo).save(captor.capture());
        // Ningún resultado coincide con año 2010 ±1, así que cae a pasada 2: JP por popularidad.
        assertThat(captor.getValue().getTmdbId()).isEqualTo(2L);
    }

    @Test
    void fallbackToAnyResultIfNoJapaneseAvailable() {
        Anime anime = anime(1L, "Title", 2020);

        TmdbSearchResult usLow = result(1L, "US", "2020-01-01", 5.0);
        TmdbSearchResult krHigh = result(2L, "KR", "2020-01-01", 50.0);

        AnimeRepository repo = mock(AnimeRepository.class);
        when(repo.findAll()).thenReturn(List.of(anime));
        TmdbClient client = mock(TmdbClient.class);
        when(client.searchTv(any())).thenReturn(
                new TmdbSearchResponse(1, List.of(usLow, krHigh), 2, 1));

        new AnimeMatchingService(client, repo).matchAll();

        ArgumentCaptor<Anime> captor = ArgumentCaptor.forClass(Anime.class);
        verify(repo).save(captor.capture());
        // Sin JP, cae a pasada 3: cualquier resultado por popularidad → KR.
        assertThat(captor.getValue().getTmdbId()).isEqualTo(2L);
    }

    @Test
    void noResultsMeansNoSave() {
        Anime anime = anime(1L, "Unknown Title", 2020);

        AnimeRepository repo = mock(AnimeRepository.class);
        when(repo.findAll()).thenReturn(List.of(anime));
        TmdbClient client = mock(TmdbClient.class);
        when(client.searchTv(any())).thenReturn(new TmdbSearchResponse(1, List.of(), 0, 0));

        int matched = new AnimeMatchingService(client, repo).matchAll();

        assertThat(matched).isZero();
        verify(repo, never()).save(any());
    }

    @Test
    void skipAnimeAlreadyMatched() {
        Anime alreadyMatched = anime(1L, "Already", 2020);
        alreadyMatched.setTmdbId(999L);

        AnimeRepository repo = mock(AnimeRepository.class);
        when(repo.findAll()).thenReturn(List.of(alreadyMatched));
        TmdbClient client = mock(TmdbClient.class);

        int matched = new AnimeMatchingService(client, repo).matchAll();

        assertThat(matched).isZero();
        // No se debe consultar TMDb ni guardar nada.
        verify(client, never()).searchTv(any());
        verify(repo, never()).save(any());
    }

    // --- helpers ---

    private static Anime anime(long id, String titleEnglish, int startYear) {
        Anime a = new Anime();
        a.setId(id);
        a.setAnilistId(id);
        a.setTitleEnglish(titleEnglish);
        a.setStartYear(startYear);
        a.setSlug("slug-" + id);
        return a;
    }

    private static TmdbSearchResult result(long id, String origin, String firstAirDate, double popularity) {
        return new TmdbSearchResult(id, "Name " + id, "Orig " + id, "overview",
                firstAirDate, List.of(origin), null, popularity);
    }
}
