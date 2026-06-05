package com.dondeanime.backend.anime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.dondeanime.animetitlematcher.api.AnimeTitleMatcher;
import io.github.diegoalegil.tsunagi.tmdb.TmdbClient;
import io.github.diegoalegil.tsunagi.tmdb.TmdbSearchResponse;
import io.github.diegoalegil.tsunagi.tmdb.TmdbSearchResult;

/**
 * Tests unitarios del matching contra TMDb.
 *
 * <p>Mockeamos TmdbClient para fabricar respuestas reproducibles y
 * AnimeRepository para capturar lo que se guarda. El matcher es el real
 * (inmutable, sin estado), así que probamos la integración de verdad. Sin
 * Spring, sin BD.
 *
 * <p>Los resultados con título de relleno ("Name N") hacen que el matcher no
 * tenga confianza, por lo que esos tests ejercitan la heurística de respaldo;
 * los tests con títulos reales ejercitan la ruta del matcher.
 */
class AnimeMatchingServiceTest {

    private final AnimeTitleMatcher matcher = AnimeTitleMatcher.createDefault();

    private AnimeMatchingService service(TmdbClient client, AnimeRepository repo) {
        return new AnimeMatchingService(client, repo, matcher);
    }

    // --- heurística de respaldo (títulos de relleno => matcher sin confianza) ---

    @Test
    void preferJapaneseWithMatchingYearOverMorePopularWrongYear() {
        Anime anime = anime(123L, "My Hero Academia", 2016);

        TmdbSearchResult correctYear = result(65930L, "JP", "2016-04-03", 23.8);
        TmdbSearchResult wrongYearMorePopular = result(280110L, "JP", "2025-04-07", 26.2);

        AnimeRepository repo = mock(AnimeRepository.class);
        when(repo.findAllWithSynonyms()).thenReturn(List.of(anime));
        TmdbClient client = mock(TmdbClient.class);
        when(client.searchMulti(any(), any())).thenReturn(
                new TmdbSearchResponse(1, List.of(wrongYearMorePopular, correctYear), 2, 1));

        service(client, repo).matchAll();

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
        when(repo.findAllWithSynonyms()).thenReturn(List.of(anime));
        TmdbClient client = mock(TmdbClient.class);
        when(client.searchMulti(any(), any())).thenReturn(
                new TmdbSearchResponse(1, List.of(jpFar, jpFarMorePopular), 2, 1));

        service(client, repo).matchAll();

        ArgumentCaptor<Anime> captor = ArgumentCaptor.forClass(Anime.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getTmdbId()).isEqualTo(2L);
    }

    @Test
    void fallbackToAnyResultIfNoJapaneseAvailable() {
        Anime anime = anime(1L, "Title", 2020);

        TmdbSearchResult usLow = result(1L, "US", "2020-01-01", 5.0);
        TmdbSearchResult krHigh = result(2L, "KR", "2020-01-01", 50.0);

        AnimeRepository repo = mock(AnimeRepository.class);
        when(repo.findAllWithSynonyms()).thenReturn(List.of(anime));
        TmdbClient client = mock(TmdbClient.class);
        when(client.searchMulti(any(), any())).thenReturn(
                new TmdbSearchResponse(1, List.of(usLow, krHigh), 2, 1));

        service(client, repo).matchAll();

        ArgumentCaptor<Anime> captor = ArgumentCaptor.forClass(Anime.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getTmdbId()).isEqualTo(2L);
    }

    @Test
    void noResultsMeansNoSave() {
        Anime anime = anime(1L, "Unknown Title", 2020);

        AnimeRepository repo = mock(AnimeRepository.class);
        when(repo.findAllWithSynonyms()).thenReturn(List.of(anime));
        TmdbClient client = mock(TmdbClient.class);
        when(client.searchMulti(any(), any())).thenReturn(new TmdbSearchResponse(1, List.of(), 0, 0));

        int matched = service(client, repo).matchAll();

        assertThat(matched).isZero();
        verify(repo, never()).save(any());
    }

    @Test
    void skipAnimeAlreadyMatched() {
        Anime alreadyMatched = anime(1L, "Already", 2020);
        alreadyMatched.setTmdbId(999L);

        AnimeRepository repo = mock(AnimeRepository.class);
        when(repo.findAllWithSynonyms()).thenReturn(List.of(alreadyMatched));
        TmdbClient client = mock(TmdbClient.class);

        int matched = service(client, repo).matchAll();

        assertThat(matched).isZero();
        verify(client, never()).searchMulti(any(), any());
        verify(repo, never()).save(any());
    }

    // --- ruta del matcher (títulos reales) ---

    @Test
    void matcherPicksTheTitleMatchOverTheMorePopularWrongOne() {
        // El más popular tiene título equivocado; el matcher debe quedarse con
        // el que de verdad coincide aunque sea menos popular (la heurística vieja
        // habría elegido el popular por ser JP + mismo año).
        Anime anime = anime(1L, "Steins;Gate", 2011);

        TmdbSearchResult popularWrong = tvResult(111L, "Some Popular Show", "JP", "2011-01-01", 100.0);
        TmdbSearchResult correct = tvResult(222L, "Steins;Gate", "JP", "2011-04-06", 5.0);

        AnimeRepository repo = mock(AnimeRepository.class);
        when(repo.findAllWithSynonyms()).thenReturn(List.of(anime));
        TmdbClient client = mock(TmdbClient.class);
        when(client.searchMulti(any(), any())).thenReturn(
                new TmdbSearchResponse(1, List.of(popularWrong, correct), 2, 1));

        service(client, repo).matchAll();

        ArgumentCaptor<Anime> captor = ArgumentCaptor.forClass(Anime.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getTmdbId()).isEqualTo(222L);
    }

    @Test
    void matcherMatchesTheMovieAndVetoesTheMislabelledSeries() {
        Anime movie = anime(1L, "A Silent Voice", 2016);
        movie.setFormat("MOVIE");

        TmdbSearchResult asSeries = tvResult(1L, "A Silent Voice", "JP", "2016-09-17", 50.0);
        TmdbSearchResult asMovie = movieResult(2L, "A Silent Voice", "2016-09-17", 5.0);

        AnimeRepository repo = mock(AnimeRepository.class);
        when(repo.findAllWithSynonyms()).thenReturn(List.of(movie));
        TmdbClient client = mock(TmdbClient.class);
        when(client.searchMulti(any(), any())).thenReturn(
                new TmdbSearchResponse(1, List.of(asSeries, asMovie), 2, 1));

        service(client, repo).matchAll();

        ArgumentCaptor<Anime> captor = ArgumentCaptor.forClass(Anime.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getTmdbId()).isEqualTo(2L);
    }

    @Test
    void fallbackForMovieFormatPicksMovieNotSeries() {
        // Título de relleno => el matcher NO confía => entra el respaldo. Antes,
        // el respaldo solo miraba series, así que esta película habría caído a la
        // serie (id 1). Con el fix, para formato MOVIE el respaldo mira películas.
        Anime movie = anime(1L, "Some Film", 2020);
        movie.setFormat("MOVIE");

        TmdbSearchResult fillerSeries = result(1L, "JP", "2020-01-01", 100.0); // tv, ja, más popular
        TmdbSearchResult fillerMovie = movieFiller(2L, "2020-01-01", 5.0); // movie, ja, menos popular

        AnimeRepository repo = mock(AnimeRepository.class);
        when(repo.findAllWithSynonyms()).thenReturn(List.of(movie));
        TmdbClient client = mock(TmdbClient.class);
        when(client.searchMulti(any(), any())).thenReturn(
                new TmdbSearchResponse(1, List.of(fillerSeries, fillerMovie), 2, 1));

        service(client, repo).matchAll();

        ArgumentCaptor<Anime> captor = ArgumentCaptor.forClass(Anime.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getTmdbId()).isEqualTo(2L);
    }

    @Test
    void matcherMatchesThroughASynonym() {
        Anime anime = new Anime();
        anime.setId(1L);
        anime.setAnilistId(16498L);
        anime.setTitleRomaji("Shingeki no Kyojin");
        anime.setSynonyms(Set.of("Attack on Titan"));
        anime.setStartYear(2013);
        anime.setSlug("shingeki-no-kyojin");

        TmdbSearchResult popularWrong = tvResult(1L, "Some Other Anime", "JP", "2013-01-01", 100.0);
        TmdbSearchResult correct = tvResult(2L, "Attack on Titan", "JP", "2013-04-07", 5.0);

        AnimeRepository repo = mock(AnimeRepository.class);
        when(repo.findAllWithSynonyms()).thenReturn(List.of(anime));
        TmdbClient client = mock(TmdbClient.class);
        when(client.searchMulti(any(), any())).thenReturn(
                new TmdbSearchResponse(1, List.of(popularWrong, correct), 2, 1));

        service(client, repo).matchAll();

        ArgumentCaptor<Anime> captor = ArgumentCaptor.forClass(Anime.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getTmdbId()).isEqualTo(2L);
    }

    // --- dry-run ---

    @Test
    void dryRunReportsDiffsWithoutSaving() {
        Anime unmatched = anime(1L, "Steins;Gate", 2011); // sin tmdbId actual
        Anime alreadyCorrect = anime(2L, "Cowboy Bebop", 1998);
        alreadyCorrect.setTmdbId(30991L);

        AnimeRepository repo = mock(AnimeRepository.class);
        when(repo.findAllWithSynonyms()).thenReturn(List.of(unmatched, alreadyCorrect));
        TmdbClient client = mock(TmdbClient.class);
        when(client.searchMulti("Steins;Gate", "es-ES")).thenReturn(new TmdbSearchResponse(
                1, List.of(tvResult(222L, "Steins;Gate", "JP", "2011-04-06", 5.0)), 1, 1));
        when(client.searchMulti("Cowboy Bebop", "es-ES")).thenReturn(new TmdbSearchResponse(
                1, List.of(tvResult(30991L, "Cowboy Bebop", "JP", "1998-04-03", 50.0)), 1, 1));

        AnimeMatchingService.DryRunReport report = service(client, repo).dryRunMatchAll();

        assertThat(report.total()).isEqualTo(2);
        assertThat(report.changed()).isEqualTo(1);
        assertThat(report.unchanged()).isEqualTo(1);
        assertThat(report.nowMatched()).isEqualTo(1);
        assertThat(report.diffs()).singleElement().satisfies(diff -> {
            assertThat(diff.slug()).isEqualTo("steinsgate");
            assertThat(diff.currentTmdbId()).isNull();
            assertThat(diff.proposedTmdbId()).isEqualTo(222L);
        });
        verify(repo, never()).save(any());
    }

    // --- rematch ---

    @Test
    void rematchReprocessesAlreadyMatchedAnime() {
        Anime anime = anime(1L, "My Hero Academia", 2016);
        anime.setTmdbId(999L);
        TmdbSearchResult correctYear = result(65930L, "JP", "2016-04-03", 23.8);

        AnimeRepository repo = mock(AnimeRepository.class);
        when(repo.findBySlugWithSynonyms("my-hero-academia")).thenReturn(Optional.of(anime));
        TmdbClient client = mock(TmdbClient.class);
        when(client.searchMulti(any(), any())).thenReturn(new TmdbSearchResponse(1, List.of(correctYear), 1, 1));

        Optional<AnimeMatchingService.RematchResult> result =
                service(client, repo).rematch("my-hero-academia");

        ArgumentCaptor<Anime> captor = ArgumentCaptor.forClass(Anime.class);
        verify(repo).save(captor.capture());
        assertThat(result).isPresent();
        assertThat(result.get().matched()).isTrue();
        assertThat(captor.getValue().getTmdbId()).isEqualTo(65930L);
    }

    @Test
    void rematchClearsTmdbIdWhenNoResultExists() {
        Anime anime = anime(1L, "Unknown Title", 2020);
        anime.setTmdbId(999L);

        AnimeRepository repo = mock(AnimeRepository.class);
        when(repo.findBySlugWithSynonyms("unknown-title")).thenReturn(Optional.of(anime));
        TmdbClient client = mock(TmdbClient.class);
        when(client.searchMulti(any(), any())).thenReturn(new TmdbSearchResponse(1, List.of(), 0, 0));

        Optional<AnimeMatchingService.RematchResult> result =
                service(client, repo).rematch("unknown-title");

        ArgumentCaptor<Anime> captor = ArgumentCaptor.forClass(Anime.class);
        verify(repo).save(captor.capture());
        assertThat(result).isPresent();
        assertThat(result.get().matched()).isFalse();
        assertThat(captor.getValue().getTmdbId()).isNull();
    }

    @Test
    void rematchUnknownSlugDoesNothing() {
        AnimeRepository repo = mock(AnimeRepository.class);
        when(repo.findBySlugWithSynonyms("missing")).thenReturn(Optional.empty());
        TmdbClient client = mock(TmdbClient.class);

        Optional<AnimeMatchingService.RematchResult> result =
                service(client, repo).rematch("missing");

        assertThat(result).isEmpty();
        verify(client, never()).searchMulti(any(), any());
        verify(repo, never()).save(any());
    }

    // --- helpers ---

    private static Anime anime(long id, String titleEnglish, int startYear) {
        Anime a = new Anime();
        a.setId(id);
        a.setAnilistId(id);
        a.setTitleEnglish(titleEnglish);
        a.setStartYear(startYear);
        a.setSlug(titleEnglish.toLowerCase().replace(";", "").replace(" ", "-"));
        return a;
    }

    /** Resultado de serie con título de relleno ("Name N") => no matchea. */
    private static TmdbSearchResult result(long id, String origin, String firstAirDate, double popularity) {
        return new TmdbSearchResult(id, "Name " + id, "Orig " + id, "overview",
                firstAirDate, List.of(origin), null, popularity,
                null, null, null, "tv", "ja");
    }

    /** Resultado de serie con título real. */
    private static TmdbSearchResult tvResult(long id, String name, String origin, String firstAirDate, double popularity) {
        return new TmdbSearchResult(id, name, name, "overview",
                firstAirDate, List.of(origin), null, popularity,
                null, null, null, "tv", "ja");
    }

    /** Resultado de película con título real. */
    private static TmdbSearchResult movieResult(long id, String title, String releaseDate, double popularity) {
        return new TmdbSearchResult(id, null, null, "overview",
                null, null, null, popularity,
                title, title, releaseDate, "movie", "ja");
    }

    /** Resultado de película con título de relleno ("Name N") => no matchea. */
    private static TmdbSearchResult movieFiller(long id, String releaseDate, double popularity) {
        return new TmdbSearchResult(id, null, null, "overview",
                null, null, null, popularity,
                "Name " + id, "Orig " + id, releaseDate, "movie", "ja");
    }
}
