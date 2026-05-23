package com.dondeanime.backend.anime;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dondeanime.backend.anime.tmdb.TmdbClient;
import com.dondeanime.backend.anime.tmdb.TmdbSearchResponse;
import com.dondeanime.backend.anime.tmdb.TmdbSearchResult;

/**
 * Cruce AniList ↔ TMDb. Para cada anime en BD sin tmdbId, busca en
 * TMDb por título inglés (fallback romaji) y guarda el ID del primer
 * resultado. Heurística simple; los matches malos se corregirán a
 * mano en mes 3 (top 50).
 *
 * Idempotente: si un anime ya tiene tmdbId, se salta.
 * Sin @Transactional: cada save es su propia tx, un fallo no
 * arrastra al resto.
 */
@Service
public class AnimeMatchingService {

    private static final Logger log = LoggerFactory.getLogger(AnimeMatchingService.class);

    /**
     * TMDb permite ~40 req/10s. 300ms entre llamadas = ~33 req/10s
     * con margen para que un pico no nos meta en throttling.
     */
    private static final long RATE_LIMIT_SLEEP_MS = 300;

    private final TmdbClient client;
    private final AnimeRepository repository;

    public AnimeMatchingService(TmdbClient client, AnimeRepository repository) {
        this.client = client;
        this.repository = repository;
    }

    public int matchAll() {
        List<Anime> animes = repository.findAll();
        log.info("Matching TMDb iniciado: {} anime a procesar", animes.size());

        int matched = 0;
        int skipped = 0;
        int failed = 0;

        for (Anime a : animes) {
            if (a.getTmdbId() != null) {
                skipped++;
                continue;
            }
            try {
                Long tmdbId = findTmdbId(a);
                if (tmdbId != null) {
                    a.setTmdbId(tmdbId);
                    repository.save(a);
                    matched++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                log.error("Error matching slug={}: {}", a.getSlug(), e.getMessage());
                failed++;
            }
            sleep(RATE_LIMIT_SLEEP_MS);
        }

        log.info("Matching completado: {} match nuevos, {} sin match, {} ya matcheados previamente",
                matched, failed, skipped);
        return matched;
    }

    private Long findTmdbId(Anime anime) {
        String title = anime.getTitleEnglish();
        if (isBlank(title)) {
            title = anime.getTitleRomaji();
        }
        if (isBlank(title)) {
            log.warn("Anime id={} no tiene título usable, skip", anime.getId());
            return null;
        }

        TmdbSearchResponse resp = client.searchTv(title);
        if (resp == null || resp.results() == null || resp.results().isEmpty()) {
            log.warn("TMDb sin resultados para '{}' (slug={})", title, anime.getSlug());
            return null;
        }

        // Heurística en tres pasadas, cada una más permisiva:
        //   1. Origen JP + año de estreno coincide con AniList (±1 año).
        //   2. Origen JP, cualquier año.
        //   3. Cualquier resultado.
        // Dentro de cada pasada elegimos el más popular.
        //
        // Por qué la fecha: la "popularity" de TMDb es muy dinámica y un
        // spin-off recién estrenado puede tener boost de novedad encima
        // de la serie original (caso real: "My Hero Academia: Vigilantes"
        // 2025 ganaba a "My Hero Academia" 2016 si solo miramos popularidad).
        Integer animeYear = anime.getStartYear();
        Comparator<TmdbSearchResult> byPopDesc = Comparator.comparingDouble(
                r -> Optional.ofNullable(r.popularity()).orElse(0.0));

        Optional<TmdbSearchResult> best = resp.results().stream()
                .filter(AnimeMatchingService::isJapanese)
                .filter(r -> yearMatches(r, animeYear))
                .max(byPopDesc)
                .or(() -> resp.results().stream()
                        .filter(AnimeMatchingService::isJapanese)
                        .max(byPopDesc))
                .or(() -> resp.results().stream().max(byPopDesc));

        return best.map(TmdbSearchResult::id).orElse(null);
    }

    private static boolean isJapanese(TmdbSearchResult r) {
        return r.originCountry() != null && r.originCountry().contains("JP");
    }

    private static boolean yearMatches(TmdbSearchResult r, Integer animeYear) {
        if (animeYear == null || r.firstAirDate() == null || r.firstAirDate().length() < 4) {
            return false;
        }
        try {
            int tmdbYear = Integer.parseInt(r.firstAirDate().substring(0, 4));
            return Math.abs(tmdbYear - animeYear) <= 1;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
