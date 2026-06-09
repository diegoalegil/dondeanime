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

import io.github.diegoalegil.tsunagi.tmdb.TmdbClient;
import io.github.diegoalegil.tsunagi.tmdb.TmdbVideo;
import io.github.diegoalegil.tsunagi.tmdb.TmdbVideosResponse;

class TrailerSyncServiceTest {

    @Test
    void storesFirstYoutubeTrailerKey() {
        Anime anime = anime(1L, 100L);
        AnimeRepository repository = mock(AnimeRepository.class);
        when(repository.findAll()).thenReturn(List.of(anime));

        TmdbClient client = mock(TmdbClient.class);
        when(client.getTrailers(100L, "es-ES")).thenReturn(new TmdbVideosResponse(100L, List.of(
                new TmdbVideo("clip-ignored", "YouTube", "Clip", "Clip"),
                new TmdbVideo("vimeo-ignored", "Vimeo", "Trailer", "Trailer"),
                new TmdbVideo("abc123DEF45", "YouTube", "Trailer", "Trailer español"),
                new TmdbVideo("later", "YouTube", "Trailer", "Otro trailer")
        )));

        int processed = new TrailerSyncService(client, repository).syncAll();

        ArgumentCaptor<Anime> captor = ArgumentCaptor.forClass(Anime.class);
        verify(repository).save(captor.capture());
        assertThat(processed).isEqualTo(1);
        assertThat(captor.getValue().getTrailerYoutubeId()).isEqualTo("abc123DEF45");
    }

    @Test
    void clearsExistingTrailerWhenTmdbHasNoYoutubeTrailer() {
        Anime anime = anime(1L, 100L);
        anime.setTrailerYoutubeId("oldTrailer");
        AnimeRepository repository = mock(AnimeRepository.class);
        when(repository.findAll()).thenReturn(List.of(anime));

        TmdbClient client = mock(TmdbClient.class);
        when(client.getTrailers(100L, "es-ES")).thenReturn(new TmdbVideosResponse(100L, List.of(
                new TmdbVideo("featurette", "YouTube", "Featurette", "Making of")
        )));

        new TrailerSyncService(client, repository).syncAll();

        ArgumentCaptor<Anime> captor = ArgumentCaptor.forClass(Anime.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTrailerYoutubeId()).isNull();
    }

    @Test
    void skipsAnimeWithoutTmdbId() {
        Anime anime = anime(1L, null);
        AnimeRepository repository = mock(AnimeRepository.class);
        when(repository.findAll()).thenReturn(List.of(anime));
        TmdbClient client = mock(TmdbClient.class);

        int processed = new TrailerSyncService(client, repository).syncAll();

        assertThat(processed).isZero();
        verify(client, never()).getTrailers(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void skipsMovieFormatAnime() {
        // getTrailers pega a /tv/{id}/videos: con un tmdbId de película
        // devolvería los vídeos de una serie sin relación.
        Anime anime = anime(1L, 100L);
        anime.setFormat("MOVIE");
        AnimeRepository repository = mock(AnimeRepository.class);
        when(repository.findAll()).thenReturn(List.of(anime));
        TmdbClient client = mock(TmdbClient.class);

        int processed = new TrailerSyncService(client, repository).syncAll();

        assertThat(processed).isZero();
        verify(client, never()).getTrailers(any(), any());
        verify(repository, never()).save(any());
    }

    private static Anime anime(Long id, Long tmdbId) {
        Anime anime = new Anime();
        anime.setId(id);
        anime.setAnilistId(id);
        anime.setSlug("anime-" + id);
        anime.setTitleEnglish("Anime " + id);
        anime.setTmdbId(tmdbId);
        return anime;
    }
}
