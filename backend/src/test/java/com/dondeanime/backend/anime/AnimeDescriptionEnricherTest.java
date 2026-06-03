package com.dondeanime.backend.anime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.github.diegoalegil.tsunagi.tmdb.TmdbClient;
import io.github.diegoalegil.tsunagi.tmdb.TmdbTvDetailsResponse;

class AnimeDescriptionEnricherTest {

    @Test
    void enrichesMissingSpanishDescriptionFromTmdb() {
        Anime anime = anime();
        AnimeRepository repository = mock(AnimeRepository.class);
        TmdbClient client = mock(TmdbClient.class);
        when(client.getTvDetails(99L, "es-ES")).thenReturn(new TmdbTvDetailsResponse(" Sinopsis en espanol "));

        boolean enriched = new AnimeDescriptionEnricher(repository, client).enrichOne(anime);

        assertThat(enriched).isTrue();
        ArgumentCaptor<Anime> captor = ArgumentCaptor.forClass(Anime.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getDescriptionEs()).isEqualTo("Sinopsis en espanol");
    }

    @Test
    void skipsAnimeWithoutTmdbId() {
        Anime anime = anime();
        anime.setTmdbId(null);
        AnimeRepository repository = mock(AnimeRepository.class);
        TmdbClient client = mock(TmdbClient.class);

        boolean enriched = new AnimeDescriptionEnricher(repository, client).enrichOne(anime);

        assertThat(enriched).isFalse();
        verify(client, never()).getTvDetails(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void skipsWhenSpanishDescriptionAlreadyExists() {
        Anime anime = anime();
        anime.setDescriptionEs("Ya existe");
        AnimeRepository repository = mock(AnimeRepository.class);
        TmdbClient client = mock(TmdbClient.class);

        boolean enriched = new AnimeDescriptionEnricher(repository, client).enrichOne(anime);

        assertThat(enriched).isFalse();
        verify(client, never()).getTvDetails(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void batchUsesRepositoryQueryAndCountsSavedDescriptions() {
        Anime first = anime();
        Anime second = anime();
        second.setId(2L);
        second.setSlug("empty-overview");

        AnimeRepository repository = mock(AnimeRepository.class);
        TmdbClient client = mock(TmdbClient.class);
        when(repository.findWithTmdbIdAndMissingDescriptionEs()).thenReturn(List.of(first, second));
        when(client.getTvDetails(eq(99L), eq("es-ES")))
                .thenReturn(new TmdbTvDetailsResponse("Sinopsis"))
                .thenReturn(new TmdbTvDetailsResponse(""));

        int enriched = new AnimeDescriptionEnricher(repository, client).enrichMissingSpanishDescriptions();

        assertThat(enriched).isEqualTo(1);
        verify(repository).save(first);
        verify(repository, never()).save(second);
    }

    private static Anime anime() {
        Anime anime = new Anime();
        anime.setId(1L);
        anime.setSlug("attack-on-titan");
        anime.setTmdbId(99L);
        return anime;
    }
}
