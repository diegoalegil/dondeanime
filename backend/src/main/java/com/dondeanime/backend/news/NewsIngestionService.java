package com.dondeanime.backend.news;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import io.github.diegoalegil.animefeed.text.DeduplicationKeyBuilder;
import io.github.diegoalegil.animefeed.text.TextNormalizer;
import io.github.diegoalegil.animefeed.text.UrlNormalizer;

/**
 * Lee los feeds de las fuentes activas y guarda cada entrada nueva como
 * {@link NewsStatus#DRAFT}, en su idioma original. El enriquecimiento editorial
 * y el match con un anime corren después, con flags independientes.
 *
 * Deliberadamente NO es {@code @Transactional} a nivel de clase: cada
 * {@code save} corre en su propia transacción implícita, así el fallo de una
 * entrada (p.ej. choque de constraint en una carrera) no envenena la sesión ni
 * tumba las demás. La idempotencia la da la unique constraint sobre
 * {@code dedup_key} (URL canónica vía anime-feed-parser): una noticia ya ingerida
 * se salta aunque la URL difiera en utm_*, barra final o http/https.
 */
@Service
public class NewsIngestionService {

    private static final Logger log = LoggerFactory.getLogger(NewsIngestionService.class);

    private static final int MAX_TITLE = 200;
    private static final int MAX_ORIGINAL_TITLE = 500;
    private static final int MAX_URL = 800;
    private static final int MAX_SLUG_ATTEMPTS = 50;

    private final NewsSourceRepository sourceRepository;
    private final NewsItemRepository itemRepository;
    private final RssNewsFetcher fetcher;

    public NewsIngestionService(
            NewsSourceRepository sourceRepository,
            NewsItemRepository itemRepository,
            RssNewsFetcher fetcher) {
        this.sourceRepository = sourceRepository;
        this.itemRepository = itemRepository;
        this.fetcher = fetcher;
    }

    /** Ingesta todas las fuentes RSS activas. No lanza: agrega errores en el resultado. */
    public NewsIngestionResult ingestAll() {
        List<NewsSource> sources = sourceRepository.findByEnabledTrue().stream()
                .filter(source -> source.getType() == NewsSourceType.RSS)
                .toList();

        List<NewsIngestionResult.SourceResult> perSource = new ArrayList<>();
        int totalCreated = 0;
        int totalSkipped = 0;
        int totalErrors = 0;

        for (NewsSource source : sources) {
            NewsIngestionResult.SourceResult result = ingestSource(source);
            perSource.add(result);
            totalCreated += result.created();
            totalSkipped += result.skipped();
            totalErrors += result.errors();
        }

        log.info("[news] ingesta completada: {} fuentes, {} nuevas, {} saltadas, {} errores",
                sources.size(), totalCreated, totalSkipped, totalErrors);
        return new NewsIngestionResult(sources.size(), totalCreated, totalSkipped, totalErrors, perSource);
    }

    private NewsIngestionResult.SourceResult ingestSource(NewsSource source) {
        List<FetchedNewsItem> fetched;
        try {
            fetched = fetcher.fetch(source.getUrl());
        } catch (RuntimeException e) {
            log.error("[news] fuente '{}' fallo al leer feed: {}", source.getName(), e.getMessage());
            return new NewsIngestionResult.SourceResult(source.getName(), 0, 0, 0, 0, false);
        }

        int created = 0;
        int skipped = 0;
        int errors = 0;
        // Evita duplicados dentro del propio feed (a veces repiten URL).
        Set<String> seenInBatch = new HashSet<>();
        for (FetchedNewsItem item : fetched) {
            String url = item.url();
            // Dedup por clave canónica (URL normalizada): mata utm_*, barra final,
            // http/https y reescrituras de feedburner que el source_url crudo dejaba pasar.
            String dedupKey = dedupKey(item, source);
            if (url.length() > MAX_URL || !seenInBatch.add(dedupKey) || itemRepository.existsByDedupKey(dedupKey)) {
                skipped++;
                continue;
            }
            switch (persist(source, item, dedupKey)) {
                case CREATED -> created++;
                case SKIPPED -> skipped++;
                case ERROR -> errors++;
            }
        }

        markFetched(source);
        log.info("[news] fuente '{}': {} leidas, {} nuevas, {} saltadas, {} errores",
                source.getName(), fetched.size(), created, skipped, errors);
        return new NewsIngestionResult.SourceResult(
                source.getName(), fetched.size(), created, skipped, errors, errors == 0);
    }

    private enum PersistOutcome { CREATED, SKIPPED, ERROR }

    private PersistOutcome persist(NewsSource source, FetchedNewsItem fetched, String dedupKey) {
        try {
            NewsItem item = new NewsItem();
            item.setSlug(uniqueSlug(fetched.title()));
            item.setTitle(truncate(fetched.title(), MAX_TITLE));
            item.setOriginalTitle(truncate(fetched.title(), MAX_ORIGINAL_TITLE));
            item.setOriginalExcerpt(fetched.excerpt());
            item.setSourceUrl(fetched.url());
            item.setSourceName(source.getName());
            item.setImageUrl(withinLength(fetched.imageUrl(), MAX_URL));
            item.setDedupKey(dedupKey);
            item.setContentHash(TextNormalizer.contentHash(
                    TextNormalizer.normalizeTitle(fetched.title()), fetched.excerpt()));
            item.setStatus(NewsStatus.DRAFT);
            item.setAutoGenerated(true);
            item.setPublishedAt(fetched.publishedAt());
            itemRepository.save(item);
            return PersistOutcome.CREATED;
        } catch (DataIntegrityViolationException e) {
            // Carrera benigna: otra pasada insertó la misma URL/slug entre el check y el save.
            log.debug("[news] '{}' ya existe (carrera de constraint), saltada", fetched.url());
            return PersistOutcome.SKIPPED;
        } catch (RuntimeException e) {
            // Fallo real de persistencia (pool agotado, timeout, lock): NO es "sin novedades".
            log.error("[news] error guardando '{}' de '{}': {}",
                    fetched.url(), source.getName(), e.getMessage());
            return PersistOutcome.ERROR;
        }
    }

    /** Clave de dedup canónica vía anime-feed-parser: URL normalizada (con fallback fuente+título). */
    private static String dedupKey(FetchedNewsItem item, NewsSource source) {
        String canonicalUrl = UrlNormalizer.normalize(item.url()).orElse(null);
        String normalizedTitle = TextNormalizer.normalizeTitle(item.title());
        return DeduplicationKeyBuilder.build(canonicalUrl, source.getName(), normalizedTitle);
    }

    private void markFetched(NewsSource source) {
        try {
            source.setLastFetchedAt(java.time.Instant.now());
            sourceRepository.save(source);
        } catch (RuntimeException e) {
            log.warn("[news] no se pudo actualizar last_fetched_at de '{}': {}",
                    source.getName(), e.getMessage());
        }
    }

    /** Genera un slug único; si choca, añade sufijo numérico. */
    private String uniqueSlug(String title) {
        String base = NewsItem.slugify(title);
        if (base.isBlank()) {
            base = "noticia";
        }
        if (!itemRepository.existsBySlug(base)) {
            return base;
        }
        for (int i = 2; i < MAX_SLUG_ATTEMPTS; i++) {
            String candidate = truncate(base, MAX_TITLE - 4) + "-" + i;
            if (!itemRepository.existsBySlug(candidate)) {
                return candidate;
            }
        }
        // Último recurso: marca temporal para no bloquear la ingesta.
        return truncate(base, MAX_TITLE - 14) + "-" + System.currentTimeMillis();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }

    private static String withinLength(String value, int max) {
        return (value != null && value.length() > max) ? null : value;
    }
}
