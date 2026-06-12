package com.dondeanime.backend.news;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resuelve revisiones editoriales (Publicar/Descartar). Idempotente: solo
 * transiciona desde PENDING_REVIEW; un callback repetido (Telegram reintenta,
 * doble tap) devuelve ALREADY_RESOLVED sin tocar nada. El peor caso de un
 * secret de webhook filtrado se limita a resolver ítems ya en revisión.
 */
@Service
public class NewsReviewService {

    public enum ReviewOutcome {
        PUBLISHED,
        DISCARDED,
        ALREADY_RESOLVED,
        NOT_FOUND
    }

    public record ReviewDecision(ReviewOutcome outcome, NewsItem item) {
    }

    private final NewsItemRepository itemRepository;

    public NewsReviewService(NewsItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Transactional
    public ReviewDecision publish(long id) {
        return resolve(id, NewsStatus.PUBLISHED, ReviewOutcome.PUBLISHED);
    }

    @Transactional
    public ReviewDecision discard(long id) {
        return resolve(id, NewsStatus.DISCARDED, ReviewOutcome.DISCARDED);
    }

    private ReviewDecision resolve(long id, NewsStatus target, ReviewOutcome outcome) {
        Optional<NewsItem> found = itemRepository.findById(id);
        if (found.isEmpty()) {
            return new ReviewDecision(ReviewOutcome.NOT_FOUND, null);
        }
        NewsItem item = found.get();
        if (item.getStatus() != NewsStatus.PENDING_REVIEW) {
            return new ReviewDecision(ReviewOutcome.ALREADY_RESOLVED, item);
        }
        item.setStatus(target);
        if (target == NewsStatus.PUBLISHED && item.getPublishedAt() == null) {
            // Mismo criterio que el publicado automático: fecha del fetch.
            item.setPublishedAt(item.getFetchedAt() != null ? item.getFetchedAt() : Instant.now());
        }
        itemRepository.save(item);
        return new ReviewDecision(outcome, item);
    }
}
