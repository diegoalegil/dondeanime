package com.dondeanime.backend.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;

class NewsServiceTest {

    private final NewsItemRepository itemRepository = mock(NewsItemRepository.class);
    private final AnimeRepository animeRepository = mock(AnimeRepository.class);
    private final NewsService service = new NewsService(itemRepository, animeRepository);

    @Test
    void latestPublishedIncludesPublicAnimeSlug() {
        NewsItem item = newsItem(5L);
        Anime anime = anime(5L, "solo-leveling");
        when(itemRepository.findByStatusOrderByPublishedAtDesc(
                org.mockito.ArgumentMatchers.eq(NewsStatus.PUBLISHED),
                org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of(item));
        when(animeRepository.findAllById(Set.of(5L))).thenReturn(List.of(anime));

        List<NewsSummaryDto> result = service.latestPublished(30);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().animeId()).isEqualTo(5L);
        assertThat(result.getFirst().animeSlug()).isEqualTo("solo-leveling");
    }

    @Test
    void publishedSlugsReturnsAllWithoutLimit() {
        when(itemRepository.findSlugsByStatus(NewsStatus.PUBLISHED))
                .thenReturn(List.of("noticia-nueva", "noticia-vieja"));

        assertThat(service.publishedSlugs()).containsExactly("noticia-nueva", "noticia-vieja");
    }

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

    private static NewsItem newsItem(Long animeId) {
        NewsItem item = new NewsItem();
        item.setSlug("solo-leveling-season-2");
        item.setTitle("Solo Leveling season 2");
        item.setSourceName("ANN");
        item.setSourceUrl("https://news.example/solo-leveling-season-2");
        item.setAnimeId(animeId);
        item.setStatus(NewsStatus.PUBLISHED);
        item.setPublishedAt(Instant.parse("2026-06-01T12:00:00Z"));
        return item;
    }

    private static Anime anime(Long id, String slug) {
        Anime anime = new Anime();
        anime.setId(id);
        anime.setSlug(slug);
        return anime;
    }
}
