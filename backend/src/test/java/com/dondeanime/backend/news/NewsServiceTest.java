package com.dondeanime.backend.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;

class NewsServiceTest {

    private final NewsItemRepository itemRepository = mock(NewsItemRepository.class);
    private final AnimeRepository animeRepository = mock(AnimeRepository.class);
    private final NewsService service = new NewsService(itemRepository, animeRepository);

    @Test
    void publishedForAnimeSlugResolvesSlugToInternalId() {
        Anime anime = mock(Anime.class);
        when(anime.getId()).thenReturn(42L);
        when(animeRepository.findBySlug("attack-on-titan")).thenReturn(Optional.of(anime));
        when(itemRepository.findByAnimeIdAndStatusOrderByPublishedAtDesc(42L, NewsStatus.PUBLISHED))
                .thenReturn(List.of());

        List<NewsSummaryDto> result = service.publishedForAnimeSlug("attack-on-titan");

        assertThat(result).isEmpty();
        verify(itemRepository).findByAnimeIdAndStatusOrderByPublishedAtDesc(42L, NewsStatus.PUBLISHED);
    }

    @Test
    void publishedForAnimeSlugReturnsEmptyWhenSlugUnknown() {
        when(animeRepository.findBySlug("nope")).thenReturn(Optional.empty());

        List<NewsSummaryDto> result = service.publishedForAnimeSlug("nope");

        assertThat(result).isEmpty();
        verify(itemRepository, never())
                .findByAnimeIdAndStatusOrderByPublishedAtDesc(org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any());
    }
}
