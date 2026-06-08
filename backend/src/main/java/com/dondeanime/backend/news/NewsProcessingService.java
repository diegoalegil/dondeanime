package com.dondeanime.backend.news;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;

@Service
public class NewsProcessingService {

    private static final int MAX_ITEMS_PER_RUN = 100;
    private static final int MAX_SUMMARY = 500;
    private static final int MAX_META_TITLE = 70;
    private static final int MAX_META_DESCRIPTION = 160;
    private static final int MIN_TITLE_MATCH_LENGTH = 4;

    private final NewsItemRepository itemRepository;
    private final AnimeRepository animeRepository;
    private final boolean enabled;
    private final boolean publish;
    private final int maxItems;

    public NewsProcessingService(
            NewsItemRepository itemRepository,
            AnimeRepository animeRepository,
            @Value("${news.processing.enabled:false}") boolean enabled,
            @Value("${news.processing.publish:false}") boolean publish,
            @Value("${news.processing.max-items:20}") int maxItems) {
        this.itemRepository = itemRepository;
        this.animeRepository = animeRepository;
        this.enabled = enabled;
        this.publish = publish;
        this.maxItems = maxItems;
    }

    @Transactional
    public NewsProcessingResult processDrafts() {
        if (!enabled) {
            return new NewsProcessingResult(false, 0, 0, 0, 0, 0);
        }

        List<NewsItem> drafts = itemRepository.findByStatusOrderByFetchedAtAsc(
                NewsStatus.DRAFT, PageRequest.of(0, effectiveLimit()));
        if (drafts.isEmpty()) {
            return new NewsProcessingResult(true, 0, 0, 0, 0, 0);
        }

        List<Anime> animeCatalog = animeRepository.findAllWithSynonyms();
        int processed = 0;
        int published = 0;
        int matched = 0;
        int skipped = 0;

        for (NewsItem item : drafts) {
            boolean changed = false;
            String baseSummary = firstText(item.getOriginalExcerpt(), item.getSummary(), item.getTitle());
            if (!hasText(baseSummary)) {
                skipped++;
                continue;
            }

            if (!hasText(item.getSummary())) {
                item.setSummary(truncate(baseSummary, MAX_SUMMARY));
                changed = true;
            }

            if (!hasText(item.getBody())) {
                item.setBody(toBodyHtml(item.getSummary()));
                changed = true;
            }

            if (!hasText(item.getMetaTitle())) {
                item.setMetaTitle(truncate(item.getTitle(), MAX_META_TITLE));
                changed = true;
            }

            if (!hasText(item.getMetaDescription())) {
                item.setMetaDescription(truncate(item.getSummary(), MAX_META_DESCRIPTION));
                changed = true;
            }

            if (item.getAnimeId() == null) {
                Optional<Anime> anime = bestAnimeMatch(item, animeCatalog);
                if (anime.isPresent()) {
                    item.setAnimeId(anime.get().getId());
                    matched++;
                    changed = true;
                }
            }

            if (publish && canPublish(item)) {
                item.setStatus(NewsStatus.PUBLISHED);
                if (item.getPublishedAt() == null) {
                    item.setPublishedAt(firstInstant(item.getFetchedAt(), Instant.now()));
                }
                published++;
                changed = true;
            }

            if (changed) {
                itemRepository.save(item);
                processed++;
            } else {
                skipped++;
            }
        }

        return new NewsProcessingResult(true, drafts.size(), processed, published, matched, skipped);
    }

    private int effectiveLimit() {
        return Math.max(1, Math.min(maxItems, MAX_ITEMS_PER_RUN));
    }

    private static boolean canPublish(NewsItem item) {
        return hasText(item.getTitle())
                && hasText(item.getSummary())
                && hasText(item.getBody())
                && hasText(item.getSourceUrl());
    }

    private static Optional<Anime> bestAnimeMatch(NewsItem item, List<Anime> catalog) {
        String haystack = normalizeForMatch(
                String.join(" ",
                        firstText(item.getOriginalTitle(), item.getTitle()),
                        firstText(item.getOriginalExcerpt(), item.getSummary())));
        if (!hasText(haystack)) {
            return Optional.empty();
        }
        String paddedHaystack = " " + haystack + " ";
        return catalog.stream()
                .filter(anime -> matchScore(anime, paddedHaystack) > 0)
                .max(Comparator
                        .comparingInt((Anime anime) -> matchScore(anime, paddedHaystack))
                        .thenComparingInt(anime -> Optional.ofNullable(anime.getPopularity()).orElse(0)));
    }

    private static int matchScore(Anime anime, String paddedHaystack) {
        return titlesFor(anime).stream()
                .map(NewsProcessingService::normalizeForMatch)
                .filter(title -> title.length() >= MIN_TITLE_MATCH_LENGTH)
                .filter(title -> paddedHaystack.contains(" " + title + " "))
                .mapToInt(String::length)
                .max()
                .orElse(0);
    }

    private static List<String> titlesFor(Anime anime) {
        List<String> titles = new ArrayList<>();
        addIfPresent(titles, anime.getTitleEnglish());
        addIfPresent(titles, anime.getTitleRomaji());
        addIfPresent(titles, anime.getTitleNative());
        if (anime.getSynonyms() != null) {
            anime.getSynonyms().forEach(title -> addIfPresent(titles, title));
        }
        return titles;
    }

    private static String toBodyHtml(String summary) {
        return "<p>" + HtmlUtils.htmlEscape(summary) + "</p>";
    }

    private static String normalizeForMatch(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase()
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private static Instant firstInstant(Instant... values) {
        for (Instant value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static void addIfPresent(List<String> values, String value) {
        if (hasText(value)) {
            values.add(value);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}
