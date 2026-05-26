package com.dondeanime.backend.anime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import com.github.benmanes.caffeine.cache.Caffeine;

class RecommendationServiceTest {

    private final AnimeRepository animeRepository = mock(AnimeRepository.class);
    private final RecommendationService recommendationService = new RecommendationService(
            animeRepository,
            Caffeine.newBuilder().maximumSize(100).build());

    @Test
    void findsSimilarAnimeByPrimaryGenreThenStudioWithoutDuplicates() {
        Anime source = anime(1L, "source", 85, "Madhouse", "Drama", "Action");
        Anime genreMatch = anime(2L, "genre", 91, "Bones", "Action");
        Anime sharedMatch = anime(3L, "shared", 88, "Madhouse", "Action");
        Anime studioMatch = anime(4L, "studio", 82, "Madhouse", "Comedy");

        when(animeRepository.findById(1L)).thenReturn(Optional.of(source));
        when(animeRepository.findSimilarByPrimaryGenre(eq(1L), eq("Action"), eq(70), any(Pageable.class)))
                .thenReturn(List.of(genreMatch, sharedMatch));
        when(animeRepository.findSimilarByPrimaryStudio(eq(1L), eq("Madhouse"), eq(70), any(Pageable.class)))
                .thenReturn(List.of(sharedMatch, studioMatch));
        when(animeRepository.findSimilarBySharedHighRankTags(eq(1L), eq(70), eq(70), any(Pageable.class)))
                .thenReturn(List.of());

        List<Anime> similar = recommendationService.findSimilar(1L, 10);

        assertThat(similar).containsExactly(genreMatch, sharedMatch, studioMatch);
    }

    @Test
    void appendsSharedTagMatchesAfterGenreAndStudio() {
        Anime source = anime(1L, "source", 85, "Madhouse", "Action");
        Anime genreMatch = anime(2L, "genre", 91, "Bones", "Action");
        Anime studioMatch = anime(3L, "studio", 88, "Madhouse", "Comedy");
        Anime tagMatch = anime(4L, "tag", 84, "Trigger", "Sci-Fi");

        when(animeRepository.findById(1L)).thenReturn(Optional.of(source));
        when(animeRepository.findSimilarByPrimaryGenre(eq(1L), eq("Action"), eq(70), any(Pageable.class)))
                .thenReturn(List.of(genreMatch));
        when(animeRepository.findSimilarByPrimaryStudio(eq(1L), eq("Madhouse"), eq(70), any(Pageable.class)))
                .thenReturn(List.of(studioMatch));
        when(animeRepository.findSimilarBySharedHighRankTags(eq(1L), eq(70), eq(70), any(Pageable.class)))
                .thenReturn(List.of(tagMatch));

        List<Anime> similar = recommendationService.findSimilar(1L, 10);

        assertThat(similar).containsExactly(genreMatch, studioMatch, tagMatch);
    }

    @Test
    void respectsRequestedLimit() {
        Anime source = anime(1L, "source", 85, "Madhouse", "Action");
        Anime first = anime(2L, "first", 91, "Bones", "Action");
        Anime second = anime(3L, "second", 88, "Madhouse", "Action");

        when(animeRepository.findById(1L)).thenReturn(Optional.of(source));
        when(animeRepository.findSimilarByPrimaryGenre(eq(1L), eq("Action"), eq(70), any(Pageable.class)))
                .thenReturn(List.of(first, second));
        when(animeRepository.findSimilarByPrimaryStudio(eq(1L), eq("Madhouse"), eq(70), any(Pageable.class)))
                .thenReturn(List.of(second));
        when(animeRepository.findSimilarBySharedHighRankTags(eq(1L), eq(70), eq(70), any(Pageable.class)))
                .thenReturn(List.of());

        List<Anime> similar = recommendationService.findSimilar(1L, 1);

        assertThat(similar).containsExactly(first);
    }

    @Test
    void cachesResultsForSameAnimeAndLimit() {
        Anime source = anime(1L, "source", 85, null, "Action");
        Anime first = anime(2L, "first", 91, null, "Action");

        when(animeRepository.findById(1L)).thenReturn(Optional.of(source));
        when(animeRepository.findSimilarByPrimaryGenre(eq(1L), eq("Action"), eq(70), any(Pageable.class)))
                .thenReturn(List.of(first));
        when(animeRepository.findSimilarBySharedHighRankTags(eq(1L), eq(70), eq(70), any(Pageable.class)))
                .thenReturn(List.of());

        recommendationService.findSimilar(1L, 10);
        recommendationService.findSimilar(1L, 10);

        verify(animeRepository).findById(1L);
        verify(animeRepository).findSimilarByPrimaryGenre(eq(1L), eq("Action"), eq(70), any(Pageable.class));
    }

    @Test
    void returnsEmptyForInvalidRequest() {
        assertThat(recommendationService.findSimilar(null, 10)).isEmpty();
        assertThat(recommendationService.findSimilar(1L, 0)).isEmpty();

        verify(animeRepository, never()).findById(any());
    }

    private static Anime anime(Long id, String slug, Integer score, String studio, String... genres) {
        Anime anime = new Anime();
        anime.setId(id);
        anime.setSlug(slug);
        anime.setAverageScore(score);
        anime.setPrimaryStudio(studio);
        anime.setGenres(new LinkedHashSet<>(List.of(genres)));
        return anime;
    }
}
