package com.dondeanime.backend.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

/** Tests con mocks de la lógica de dedup, slug y conteo de errores de ingesta. */
class NewsIngestionServiceTest {

    private NewsSourceRepository sourceRepository;
    private NewsItemRepository itemRepository;
    private RssNewsFetcher fetcher;
    private NewsIngestionService service;

    private final NewsSource ann = new NewsSource("ANN", NewsSourceType.RSS, "https://ann.example/rss");

    @BeforeEach
    void setUp() {
        sourceRepository = mock(NewsSourceRepository.class);
        itemRepository = mock(NewsItemRepository.class);
        fetcher = mock(RssNewsFetcher.class);
        service = new NewsIngestionService(sourceRepository, itemRepository, fetcher);
        when(sourceRepository.findByEnabledTrue()).thenReturn(List.of(ann));
    }

    private FetchedNewsItem item(String title, String url) {
        return new FetchedNewsItem(title, url, "excerpt", null, Instant.parse("2026-05-27T10:00:00Z"));
    }

    @Test
    void skipsItemsAlreadyInDb() {
        when(fetcher.fetch(anyString())).thenReturn(List.of(
                item("Nuevo", "https://ann.example/1"),
                item("Viejo", "https://ann.example/2")));
        when(itemRepository.existsBySourceUrl("https://ann.example/1")).thenReturn(false);
        when(itemRepository.existsBySourceUrl("https://ann.example/2")).thenReturn(true);
        when(itemRepository.existsBySlug(anyString())).thenReturn(false);

        NewsIngestionResult result = service.ingestAll();

        assertThat(result.itemsCreated()).isEqualTo(1);
        assertThat(result.itemsSkipped()).isEqualTo(1);
        assertThat(result.itemsErrored()).isZero();
        verify(itemRepository, times(1)).save(any(NewsItem.class));
    }

    @Test
    void skipsDuplicateUrlWithinSameFeed() {
        when(fetcher.fetch(anyString())).thenReturn(List.of(
                item("A", "https://ann.example/dup"),
                item("B", "https://ann.example/dup")));
        when(itemRepository.existsBySourceUrl(anyString())).thenReturn(false);
        when(itemRepository.existsBySlug(anyString())).thenReturn(false);

        NewsIngestionResult result = service.ingestAll();

        assertThat(result.itemsCreated()).isEqualTo(1);
        assertThat(result.itemsSkipped()).isEqualTo(1);
        verify(itemRepository, times(1)).save(any(NewsItem.class));
    }

    @Test
    void appendsNumericSuffixOnSlugCollision() {
        when(fetcher.fetch(anyString())).thenReturn(List.of(item("Anime X", "https://ann.example/x")));
        when(itemRepository.existsBySourceUrl(anyString())).thenReturn(false);
        when(itemRepository.existsBySlug("anime-x")).thenReturn(true);
        when(itemRepository.existsBySlug("anime-x-2")).thenReturn(false);

        service.ingestAll();

        ArgumentCaptor<NewsItem> captor = ArgumentCaptor.forClass(NewsItem.class);
        verify(itemRepository).save(captor.capture());
        assertThat(captor.getValue().getSlug()).isEqualTo("anime-x-2");
    }

    @Test
    void constraintRaceCountsAsSkippedNotError() {
        when(fetcher.fetch(anyString())).thenReturn(List.of(item("A", "https://ann.example/a")));
        when(itemRepository.existsBySourceUrl(anyString())).thenReturn(false);
        when(itemRepository.existsBySlug(anyString())).thenReturn(false);
        when(itemRepository.save(any(NewsItem.class)))
                .thenThrow(new DataIntegrityViolationException("uk_news_item_source_url"));

        NewsIngestionResult result = service.ingestAll();

        assertThat(result.itemsCreated()).isZero();
        assertThat(result.itemsSkipped()).isEqualTo(1);
        assertThat(result.itemsErrored()).isZero();
    }

    @Test
    void realSaveErrorCountsAsError() {
        when(fetcher.fetch(anyString())).thenReturn(List.of(item("A", "https://ann.example/a")));
        when(itemRepository.existsBySourceUrl(anyString())).thenReturn(false);
        when(itemRepository.existsBySlug(anyString())).thenReturn(false);
        when(itemRepository.save(any(NewsItem.class)))
                .thenThrow(new RuntimeException("connection pool agotado"));

        NewsIngestionResult result = service.ingestAll();

        assertThat(result.itemsErrored()).isEqualTo(1);
        assertThat(result.itemsCreated()).isZero();
        assertThat(result.sources().get(0).ok()).isFalse();
    }

    @Test
    void feedFetchFailureDoesNotThrowAndMarksSourceNotOk() {
        when(fetcher.fetch(anyString())).thenThrow(new RuntimeException("read timeout"));

        NewsIngestionResult result = service.ingestAll();

        assertThat(result.sourcesProcessed()).isEqualTo(1);
        assertThat(result.itemsCreated()).isZero();
        assertThat(result.sources().get(0).ok()).isFalse();
        verify(itemRepository, never()).save(any());
    }
}
