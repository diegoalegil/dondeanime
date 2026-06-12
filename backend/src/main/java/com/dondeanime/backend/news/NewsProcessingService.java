package com.dondeanime.backend.news;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;

/**
 * Procesado editorial de borradores RSS. Dos rutas excluyentes por pasada:
 * - LLM activo: redacción en español vía {@link LlmNewsProcessor}. Si el LLM
 *   falla o se agota el cupo diario, el ítem queda DRAFT para la siguiente
 *   pasada — nunca se publica el inglés a medias con la heurística.
 * - LLM apagado: la heurística de siempre (rellena solo lo vacío), idéntica
 *   al comportamiento previo a la integración del LLM.
 *
 * Sin transacción de método: con LLM cada ítem implica una llamada HTTP de
 * segundos y una transacción que abarcase la pasada retendría una conexión
 * del pool minutos. Mismo patrón save-por-ítem que NewsIngestionService.
 */
@Service
public class NewsProcessingService {

    private static final int MAX_ITEMS_PER_RUN = 100;
    private static final int MAX_SUMMARY = 500;
    private static final int MAX_META_TITLE = 70;
    private static final int MAX_META_DESCRIPTION = 160;
    private static final int MIN_TITLE_MATCH_LENGTH = 4;

    private final NewsItemRepository itemRepository;
    private final AnimeRepository animeRepository;
    private final LlmNewsProcessor llmNewsProcessor;
    // ObjectProvider porque el bot es @ConditionalOnProperty: sin flag no hay bean.
    private final ObjectProvider<TelegramNewsBotService> telegramBotProvider;
    private final boolean enabled;
    private final boolean publish;
    private final int maxItems;
    private final int llmDailyLimit;

    public NewsProcessingService(
            NewsItemRepository itemRepository,
            AnimeRepository animeRepository,
            LlmNewsProcessor llmNewsProcessor,
            ObjectProvider<TelegramNewsBotService> telegramBotProvider,
            @Value("${news.processing.enabled:false}") boolean enabled,
            @Value("${news.processing.publish:false}") boolean publish,
            @Value("${news.processing.max-items:20}") int maxItems,
            @Value("${news.llm.daily-limit:50}") int llmDailyLimit) {
        this.itemRepository = itemRepository;
        this.animeRepository = animeRepository;
        this.llmNewsProcessor = llmNewsProcessor;
        this.telegramBotProvider = telegramBotProvider;
        this.enabled = enabled;
        this.publish = publish;
        this.maxItems = maxItems;
        this.llmDailyLimit = llmDailyLimit;
    }

    public NewsProcessingResult processDrafts() {
        if (!enabled) {
            return NewsProcessingResult.empty(false);
        }

        boolean llmActive = llmNewsProcessor.enabled();
        // El flujo de revisión por Telegram solo aplica con LLM activo: sin
        // redacción en español no hay nada que aprobar.
        TelegramNewsBotService bot = llmActive ? telegramBotProvider.getIfAvailable() : null;

        int sentToReview = 0;
        if (bot != null) {
            sentToReview += resendPendingReviews(bot);
        }

        List<NewsItem> drafts = itemRepository.findByStatusOrderByFetchedAtAsc(
                NewsStatus.DRAFT, PageRequest.of(0, effectiveLimit()));
        if (drafts.isEmpty()) {
            return sentToReview == 0
                    ? NewsProcessingResult.empty(true)
                    : new NewsProcessingResult(true, 0, 0, 0, 0, 0, 0, 0, sentToReview);
        }

        List<Anime> animeCatalog = animeRepository.findAllWithSynonyms();
        int llmBudget = llmActive ? remainingLlmQuota() : 0;

        int processed = 0;
        int published = 0;
        int matched = 0;
        int skipped = 0;
        int llmProcessed = 0;
        int llmFailed = 0;

        for (NewsItem item : drafts) {
            boolean changed;
            if (llmActive) {
                if (item.getLlmTokensUsed() == null) {
                    if (llmBudget <= 0) {
                        skipped++;
                        continue;
                    }
                    if (!llmNewsProcessor.enrich(item)) {
                        llmFailed++;
                        skipped++;
                        continue;
                    }
                    llmBudget--;
                    llmProcessed++;
                    changed = true;
                } else {
                    // Ya redactada en una pasada anterior (p. ej. con publish=false):
                    // no se paga el LLM dos veces, solo match/publicación.
                    changed = false;
                }
            } else {
                String baseSummary = firstText(item.getOriginalExcerpt(), item.getSummary(), item.getTitle());
                if (!hasText(baseSummary)) {
                    skipped++;
                    continue;
                }
                changed = enrichHeuristically(item, baseSummary);
            }

            if (item.getAnimeId() == null) {
                Optional<Anime> anime = bestAnimeMatch(item, animeCatalog);
                if (anime.isPresent()) {
                    item.setAnimeId(anime.get().getId());
                    matched++;
                    changed = true;
                }
            }

            if (bot != null && canPublish(item)) {
                // Revisión manual: a PENDING_REVIEW y commit ANTES de la llamada
                // HTTP; si el envío falla, telegram_message_id queda NULL y la
                // siguiente pasada lo reenvía (resendPendingReviews).
                item.setStatus(NewsStatus.PENDING_REVIEW);
                itemRepository.save(item);
                Long messageId = bot.sendReviewRequest(item);
                if (messageId != null) {
                    item.setTelegramMessageId(messageId);
                    itemRepository.save(item);
                }
                sentToReview++;
                processed++;
                continue;
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

        return new NewsProcessingResult(
                true, drafts.size(), processed, published, matched, skipped,
                llmProcessed, llmFailed, sentToReview);
    }

    /** Rellena solo lo vacío, exactamente el comportamiento previo al LLM. */
    private static boolean enrichHeuristically(NewsItem item, String baseSummary) {
        boolean changed = false;
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
        return changed;
    }

    /** Reenvía las revisiones cuyo aviso a Telegram falló en pasadas anteriores. */
    private int resendPendingReviews(TelegramNewsBotService bot) {
        List<NewsItem> pending = itemRepository.findByStatusAndTelegramMessageIdIsNullOrderByFetchedAtAsc(
                NewsStatus.PENDING_REVIEW, PageRequest.of(0, effectiveLimit()));
        int sent = 0;
        for (NewsItem item : pending) {
            Long messageId = bot.sendReviewRequest(item);
            if (messageId != null) {
                item.setTelegramMessageId(messageId);
                itemRepository.save(item);
                sent++;
            }
        }
        return sent;
    }

    /**
     * Tope de coste: cuántos ítems puede redactar el LLM hoy. Cuenta por
     * llm_tokens_used + updated_at, conservador a propósito (un ítem tocado
     * después por otra razón también cuenta; mejor gastar de menos que de más).
     */
    private int remainingLlmQuota() {
        Instant startOfToday = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        long usedToday = itemRepository.countByLlmTokensUsedIsNotNullAndUpdatedAtGreaterThanEqual(startOfToday);
        return (int) Math.max(0, llmDailyLimit - usedToday);
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

    /** Trunca sin partir surrogate pairs (emoji en excerpts RSS): un par
     *  cortado a la mitad produce un String UTF-16 inválido. */
    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        int cut = max;
        if (Character.isHighSurrogate(value.charAt(cut - 1))) {
            cut--;
        }
        return value.substring(0, cut);
    }
}
