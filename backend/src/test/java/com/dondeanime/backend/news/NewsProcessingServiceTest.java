package com.dondeanime.backend.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;

class NewsProcessingServiceTest {

    private final NewsItemRepository itemRepository = mock(NewsItemRepository.class);
    private final AnimeRepository animeRepository = mock(AnimeRepository.class);

    @Test
    void disabledProcessingDoesNotTouchRepositories() {
        NewsProcessingService service = service(false, false);

        NewsProcessingResult result = service.processDrafts();

        assertThat(result.enabled()).isFalse();
        assertThat(result.itemsProcessed()).isZero();
        verify(itemRepository, never()).findByStatusOrderByFetchedAtAsc(any(), any());
        verify(animeRepository, never()).findAllWithSynonyms();
    }

    @Test
    void enrichesDraftAndKeepsItUnpublishedByDefault() {
        NewsItem item = draft("Solo Leveling season 2 announced",
                "The Solo Leveling anime returns in January with a new trailer.");
        Anime anime = anime(7L, "solo-leveling", "Solo Leveling");
        when(itemRepository.findByStatusOrderByFetchedAtAsc(eq(NewsStatus.DRAFT), any(Pageable.class)))
                .thenReturn(List.of(item));
        when(animeRepository.findAllWithSynonyms()).thenReturn(List.of(anime));

        NewsProcessingResult result = service(true, false).processDrafts();

        assertThat(result.enabled()).isTrue();
        assertThat(result.draftsSeen()).isEqualTo(1);
        assertThat(result.itemsProcessed()).isEqualTo(1);
        assertThat(result.itemsPublished()).isZero();
        assertThat(result.animeMatched()).isEqualTo(1);
        assertThat(item.getStatus()).isEqualTo(NewsStatus.DRAFT);
        assertThat(item.getAnimeId()).isEqualTo(7L);
        assertThat(item.getSummary()).isEqualTo("The Solo Leveling anime returns in January with a new trailer.");
        assertThat(item.getBody()).isEqualTo("<p>The Solo Leveling anime returns in January with a new trailer.</p>");
        assertThat(item.getMetaTitle()).isEqualTo("Solo Leveling season 2 announced");
        assertThat(item.getMetaDescription()).contains("Solo Leveling anime returns");
        verify(itemRepository).save(item);
    }

    @Test
    void publishesOnlyWhenPublishFlagIsEnabled() {
        Instant fetchedAt = Instant.parse("2026-06-01T12:00:00Z");
        NewsItem item = draft("Frieren movie announced", "Frieren gets a new movie.");
        item.setFetchedAt(fetchedAt);
        when(itemRepository.findByStatusOrderByFetchedAtAsc(eq(NewsStatus.DRAFT), any(Pageable.class)))
                .thenReturn(List.of(item));
        when(animeRepository.findAllWithSynonyms()).thenReturn(List.of(anime(9L, "frieren", "Frieren")));

        NewsProcessingResult result = service(true, true).processDrafts();

        assertThat(result.itemsPublished()).isEqualTo(1);
        assertThat(item.getStatus()).isEqualTo(NewsStatus.PUBLISHED);
        assertThat(item.getPublishedAt()).isEqualTo(fetchedAt);
        assertThat(item.getAnimeId()).isEqualTo(9L);
        verify(itemRepository).save(item);
    }

    private NewsProcessingService service(boolean enabled, boolean publish) {
        return new NewsProcessingService(itemRepository, animeRepository, enabled, publish, 20);
    }

    private static NewsItem draft(String title, String excerpt) {
        NewsItem item = new NewsItem();
        item.setSlug(NewsItem.slugify(title));
        item.setTitle(title);
        item.setOriginalTitle(title);
        item.setOriginalExcerpt(excerpt);
        item.setSourceUrl("https://news.example/" + item.getSlug());
        item.setSourceName("ANN");
        item.setStatus(NewsStatus.DRAFT);
        return item;
    }

    private static Anime anime(Long id, String slug, String title) {
        Anime anime = new Anime();
        anime.setId(id);
        anime.setSlug(slug);
        anime.setTitleEnglish(title);
        anime.setTitleRomaji(title);
        anime.setSynonyms(Set.of(title));
        return anime;
    }
}
