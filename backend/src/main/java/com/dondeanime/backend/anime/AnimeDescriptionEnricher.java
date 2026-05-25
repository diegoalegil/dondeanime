package com.dondeanime.backend.anime;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dondeanime.backend.anime.tmdb.TmdbClient;
import com.dondeanime.backend.anime.tmdb.TmdbTvDetailsResponse;

/**
 * Completa descripciones localizadas desde TMDb es-ES para anime ya
 * cruzados con tmdbId. No sustituye overrides editoriales.
 */
@Service
public class AnimeDescriptionEnricher {

    private static final Logger log = LoggerFactory.getLogger(AnimeDescriptionEnricher.class);
    private static final long RATE_LIMIT_SLEEP_MS = 300;
    private static final String SPANISH_LOCALE = "es-ES";

    private final AnimeRepository repository;
    private final TmdbClient client;

    public AnimeDescriptionEnricher(AnimeRepository repository, TmdbClient client) {
        this.repository = repository;
        this.client = client;
    }

    public int enrichMissingSpanishDescriptions() {
        List<Anime> animes = repository.findWithTmdbIdAndMissingDescriptionEs();
        log.info("Enriquecimiento de descripciones es-ES iniciado: {} anime", animes.size());

        int enriched = 0;
        int failed = 0;

        for (Anime anime : animes) {
            try {
                if (enrichOne(anime)) {
                    enriched++;
                }
            } catch (Exception e) {
                log.error("Error enriqueciendo descripción slug={}: {}", anime.getSlug(), e.getMessage());
                failed++;
            }
            sleep(RATE_LIMIT_SLEEP_MS);
        }

        log.info("Enriquecimiento de descripciones completado: {} nuevas, {} fallidas", enriched, failed);
        return enriched;
    }

    public boolean enrichOne(Anime anime) {
        if (anime == null || anime.getTmdbId() == null || !isBlank(anime.getDescriptionEs())) {
            return false;
        }

        TmdbTvDetailsResponse response = client.getTvDetails(anime.getTmdbId(), SPANISH_LOCALE);
        if (response == null || isBlank(response.overview())) {
            return false;
        }

        anime.setDescriptionEs(response.overview().trim());
        repository.save(anime);
        return true;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
