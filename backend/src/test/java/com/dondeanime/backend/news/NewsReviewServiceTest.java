package com.dondeanime.backend.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class NewsReviewServiceTest {

    private final NewsItemRepository itemRepository = mock(NewsItemRepository.class);
    private final NewsReviewService service = new NewsReviewService(itemRepository);

    @Test
    void publishesPendingReviewWithFetchedAtAsPublishedAt() {
        Instant fetchedAt = Instant.parse("2026-06-10T08:00:00Z");
        NewsItem item = pendingItem();
        item.setFetchedAt(fetchedAt);
        when(itemRepository.findById(42L)).thenReturn(Optional.of(item));

        NewsReviewService.ReviewDecision decision = service.publish(42L);

        assertThat(decision.outcome()).isEqualTo(NewsReviewService.ReviewOutcome.PUBLISHED);
        assertThat(item.getStatus()).isEqualTo(NewsStatus.PUBLISHED);
        assertThat(item.getPublishedAt()).isEqualTo(fetchedAt);
        verify(itemRepository).save(item);
    }

    @Test
    void discardsPendingReview() {
        NewsItem item = pendingItem();
        when(itemRepository.findById(42L)).thenReturn(Optional.of(item));

        NewsReviewService.ReviewDecision decision = service.discard(42L);

        assertThat(decision.outcome()).isEqualTo(NewsReviewService.ReviewOutcome.DISCARDED);
        assertThat(item.getStatus()).isEqualTo(NewsStatus.DISCARDED);
        assertThat(item.getPublishedAt()).isNull();
        verify(itemRepository).save(item);
    }

    @Test
    void repeatedCallbackIsIdempotent() {
        NewsItem item = pendingItem();
        item.setStatus(NewsStatus.PUBLISHED);
        item.setPublishedAt(Instant.parse("2026-06-10T08:00:00Z"));
        when(itemRepository.findById(42L)).thenReturn(Optional.of(item));

        NewsReviewService.ReviewDecision decision = service.discard(42L);

        assertThat(decision.outcome()).isEqualTo(NewsReviewService.ReviewOutcome.ALREADY_RESOLVED);
        assertThat(item.getStatus()).isEqualTo(NewsStatus.PUBLISHED);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void unknownIdReturnsNotFound() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.publish(99L).outcome()).isEqualTo(NewsReviewService.ReviewOutcome.NOT_FOUND);
        verify(itemRepository, never()).save(any());
    }

    private static NewsItem pendingItem() {
        NewsItem item = new NewsItem();
        item.setId(42L);
        item.setSlug("frieren-movie");
        item.setTitle("Frieren anuncia pelicula");
        item.setSummary("Frieren tendra pelicula.");
        item.setBody("<p>Frieren tendra pelicula.</p>");
        item.setSourceUrl("https://news.example/frieren");
        item.setSourceName("ANN");
        item.setStatus(NewsStatus.PENDING_REVIEW);
        return item;
    }
}
