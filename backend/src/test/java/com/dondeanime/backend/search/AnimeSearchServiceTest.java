package com.dondeanime.backend.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;

class AnimeSearchServiceTest {

    @Test
    void trimsQueryAndLimitsResults() {
        Anime first = anime("attack-on-titan", "Attack on Titan", 999);
        Anime second = anime("attack-on-titan-season-2", "Attack on Titan Season 2", 500);
        AnimeRepository repository = mock(AnimeRepository.class);
        when(repository.findBySearchVectorMatching("ataque")).thenReturn(List.of(first, second));

        List<?> result = new AnimeSearchService(repository).search("  ataque  ", 1);

        assertThat(result).hasSize(1);
        verify(repository).findBySearchVectorMatching("ataque");
    }

    @Test
    void blankQueryReturnsEmptyListWithoutRepositoryCall() {
        AnimeRepository repository = mock(AnimeRepository.class);

        List<?> result = new AnimeSearchService(repository).search("   ", 10);

        assertThat(result).isEmpty();
        verify(repository, never()).findBySearchVectorMatching(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void slugQueryIsNormalizedForFullTextSearch() {
        Anime anime = anime("attack-on-titan", "Attack on Titan", 999);
        AnimeRepository repository = mock(AnimeRepository.class);
        when(repository.findBySearchVectorMatching("attack on titan")).thenReturn(List.of(anime));

        List<?> result = new AnimeSearchService(repository).search("attack-on-titan", 10);

        assertThat(result).hasSize(1);
        verify(repository).findBySearchVectorMatching("attack on titan");
    }

    private static Anime anime(String slug, String title, int popularity) {
        Anime anime = new Anime();
        anime.setAnilistId((long) popularity);
        anime.setSlug(slug);
        anime.setTitleEnglish(title);
        anime.setTitleRomaji(title);
        anime.setFormat("TV");
        anime.setStatus("FINISHED");
        anime.setPopularity(popularity);
        anime.setCoverImage("https://example.com/cover.jpg");
        return anime;
    }
}
