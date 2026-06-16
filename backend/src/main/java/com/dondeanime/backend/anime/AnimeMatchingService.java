package com.dondeanime.backend.anime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dondeanime.animetitlematcher.api.AnimeTitleMatcher;
import com.dondeanime.animetitlematcher.api.MatchDecision;
import com.dondeanime.animetitlematcher.api.MatchResult;
import com.dondeanime.animetitlematcher.domain.AniListAnime;
import com.dondeanime.animetitlematcher.domain.AnimeFormat;
import com.dondeanime.animetitlematcher.domain.TmdbMediaType;
import com.dondeanime.animetitlematcher.domain.TmdbTitle;
import io.github.diegoalegil.tsunagi.tmdb.TmdbClient;
import io.github.diegoalegil.tsunagi.tmdb.TmdbSearchResponse;
import io.github.diegoalegil.tsunagi.tmdb.TmdbSearchResult;

/**
 * Cruce AniList ↔ TMDb. Para cada anime en BD sin tmdbId, busca en TMDb
 * (series y películas) y resuelve el equivalente con la librería
 * {@code anime-title-matcher}.
 *
 * <p>Política: aceptamos automáticamente el candidato del matcher solo cuando
 * la decisión es {@code EXACT_MATCH} o {@code HIGH_CONFIDENCE}. Para el resto
 * (medium/low/ambiguous/no-match) caemos a la heurística histórica de "el más
 * popular priorizando origen JP + año cercano", que conserva el comportamiento
 * actual y el desempate por popularidad.
 *
 * <p>Idempotente: si un anime ya tiene tmdbId, se salta. Sin {@code @Transactional}:
 * cada save es su propia tx, un fallo no arrastra al resto.
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
    private final AnimeTitleMatcher matcher;

    public AnimeMatchingService(TmdbClient client, AnimeRepository repository, AnimeTitleMatcher matcher) {
        this.client = client;
        this.repository = repository;
        this.matcher = matcher;
    }

    public int matchAll() {
        List<Anime> animes = repository.findAllWithSynonyms();
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

    public Optional<RematchResult> rematch(String animeSlug) {
        return repository.findBySlugWithSynonyms(animeSlug)
                .map(anime -> {
                    Long tmdbId = findTmdbId(anime);
                    anime.setTmdbId(tmdbId);
                    repository.save(anime);
                    log.info("Re-match TMDb slug={}: {}", anime.getSlug(),
                            tmdbId == null ? "sin match" : "match actualizado");
                    return new RematchResult(anime.getSlug(), tmdbId != null);
                });
    }

    /**
     * Re-matchea SOLO los anime de formato MOVIE. Corrige un problema de datos
     * heredado: las películas matcheadas con la lógica antigua (que solo miraba
     * series) guardaron un tmdbId del espacio TV, y por eso
     * {@code /movie/{idTV}/watch/providers} devuelve 404. Al re-resolver,
     * {@link #resolve} prioriza películas para formato MOVIE, así que obtienen
     * un id del espacio película. No toca series.
     *
     * <p>Seguro frente a fallos puntuales de TMDb: si una película no resuelve a
     * nada, conserva su id actual en vez de borrarlo (a diferencia de
     * {@link #rematch}, pensado para un único slug bajo supervisión). Igual de
     * costoso que matchAll por película (una búsqueda + rate limit cada una),
     * pero solo recorre las películas.
     *
     * @return número de películas cuyo tmdbId cambió
     */
    public int rematchMovies() {
        List<Anime> movies = repository.findAllWithSynonyms().stream()
                .filter(AnimeMatchingService::isMovieFormat)
                .toList();
        log.info("Re-match películas iniciado: {} anime de formato MOVIE", movies.size());

        int updated = 0;
        int unchanged = 0;
        int kept = 0;
        for (Anime a : movies) {
            Long current = a.getTmdbId();
            Long proposed;
            try {
                proposed = findTmdbId(a);
            } catch (Exception e) {
                log.error("Re-match película error slug={}: {}", a.getSlug(), e.getMessage());
                continue;
            }
            if (proposed == null) {
                kept++;
            } else if (Objects.equals(current, proposed)) {
                unchanged++;
            } else {
                a.setTmdbId(proposed);
                repository.save(a);
                updated++;
            }
            sleep(RATE_LIMIT_SLEEP_MS);
        }

        log.info("Re-match películas completado: {} actualizadas, {} sin cambio, {} sin match (id conservado)",
                updated, unchanged, kept);
        return updated;
    }

    /**
     * Re-matchea todo el catálogo pero SOLO aplica el cambio cuando el matcher
     * está seguro ({@code source == MATCHER}, es decir EXACT_MATCH o
     * HIGH_CONFIDENCE). Corrige matches heredados de la heurística vieja (p.ej.
     * varias temporadas de un anime compartiendo el id equivocado) sin el
     * peligro de un re-match general: el respaldo por popularidad (baja
     * confianza) NUNCA pisa un id existente, así que matches correctos cuyo
     * único candidato nuevo es un fallback dudoso (caso real: {@code erased})
     * se quedan como están.
     *
     * <p>Por defecto NO guarda: {@code apply=false} devuelve la lista de cambios
     * que haría para revisarlos antes. Con {@code apply=true} los persiste.
     * Igual de costoso que el dry-run (una búsqueda TMDb por anime).
     */
    public RematchConfidentReport rematchConfident(boolean apply) {
        List<Anime> animes = repository.findAllWithSynonyms();
        log.info("Re-match confiado iniciado (apply={}): {} anime", apply, animes.size());

        int changed = 0;
        int unchanged = 0;
        int skipped = 0;
        List<RematchConfidentChange> changes = new ArrayList<>();

        for (Anime a : animes) {
            Long current = a.getTmdbId();
            Resolution resolution;
            try {
                resolution = resolve(a);
            } catch (Exception e) {
                log.error("Re-match confiado error slug={}: {}", a.getSlug(), e.getMessage());
                continue;
            }

            boolean confident = resolution.source() == MatchSource.MATCHER && resolution.tmdbId() != null;
            if (!confident) {
                // Respaldo por popularidad o sin match: preservamos el id actual.
                skipped++;
            } else if (Objects.equals(current, resolution.tmdbId())) {
                unchanged++;
            } else {
                changes.add(new RematchConfidentChange(a.getSlug(), current, resolution.tmdbId(),
                        resolution.decision(), resolution.score()));
                if (apply) {
                    a.setTmdbId(resolution.tmdbId());
                    repository.save(a);
                }
                changed++;
            }
            sleep(RATE_LIMIT_SLEEP_MS);
        }

        log.info("Re-match confiado completado (apply={}): {} {}, {} sin cambio, "
                        + "{} respaldo/sin match (id conservado)",
                apply, changed, apply ? "aplicados" : "propuestos", unchanged, skipped);
        return new RematchConfidentReport(animes.size(), apply, changed, unchanged, skipped, changes);
    }

    /**
     * Recorre todos los anime y calcula qué tmdbId propondría el matcher SIN
     * guardar nada. Sirve para validar el cambio comparando contra los tmdbId
     * actuales antes de activarlo. Igual de costoso que matchAll (una búsqueda
     * por anime) y respeta el rate limit.
     */
    public DryRunReport dryRunMatchAll() {
        List<Anime> animes = repository.findAllWithSynonyms();
        log.info("Dry-run matching iniciado: {} anime", animes.size());

        int changed = 0;
        int unchanged = 0;
        int nowMatched = 0;
        int nowUnmatched = 0;
        int matcherWins = 0;
        List<DryRunDiff> diffs = new ArrayList<>();

        for (Anime a : animes) {
            Long current = a.getTmdbId();
            Resolution resolution;
            try {
                resolution = resolve(a);
            } catch (Exception e) {
                log.error("Dry-run error slug={}: {}", a.getSlug(), e.getMessage());
                continue;
            }
            Long proposed = resolution.tmdbId();
            if (resolution.source() == MatchSource.MATCHER) {
                matcherWins++;
            }
            if (Objects.equals(current, proposed)) {
                unchanged++;
            } else {
                changed++;
                if (current == null) {
                    nowMatched++;
                } else if (proposed == null) {
                    nowUnmatched++;
                }
                diffs.add(new DryRunDiff(a.getSlug(), current, proposed,
                        resolution.source(), resolution.decision(), resolution.score()));
            }
            sleep(RATE_LIMIT_SLEEP_MS);
        }

        DryRunReport report = new DryRunReport(animes.size(), changed, unchanged,
                nowMatched, nowUnmatched, matcherWins, diffs);
        log.info("Dry-run completado: {}", report.summary());
        return report;
    }

    private Long findTmdbId(Anime anime) {
        return resolve(anime).tmdbId();
    }

    /**
     * Resuelve el tmdbId de un anime sin persistir nada. Reutilizable por el
     * dry-run. Indica además de dónde salió la decisión.
     */
    Resolution resolve(Anime anime) {
        String query = primaryQuery(anime);
        if (isBlank(query)) {
            log.warn("Anime id={} no tiene título usable, skip", anime.getId());
            return Resolution.none(MatchSource.NO_TITLE);
        }

        TmdbSearchResponse resp = client.searchMulti(query, "es-ES");
        List<TmdbSearchResult> results = resp == null || resp.results() == null ? List.of() : resp.results();
        if (results.isEmpty()) {
            log.warn("TMDb sin resultados para '{}' (slug={})", query, anime.getSlug());
            return Resolution.none(MatchSource.NO_RESULTS);
        }

        List<TmdbTitle> candidates = results.stream()
                .filter(r -> r.id() != null && (r.isTv() || r.isMovie()))
                .map(AnimeMatchingService::toTmdbTitle)
                .toList();

        MatchResult match = matcher.findBestMatch(toAniListAnime(anime), candidates);
        if (isConfident(match.decision()) && match.bestCandidate() != null
                && mediaTypeMatchesFormat(match.bestCandidate().tmdbTitle(), isMovieFormat(anime))) {
            return new Resolution(match.bestCandidate().tmdbTitle().id(),
                    MatchSource.MATCHER, match.decision(), match.score());
        }

        Long fallback = fallbackTmdbId(results, anime.getStartYear(), isMovieFormat(anime));
        MatchSource source = fallback == null ? MatchSource.NONE : MatchSource.FALLBACK;
        return new Resolution(fallback, source, match.decision(), match.score());
    }

    /** Solo aceptamos automáticamente las decisiones fuertes del matcher. */
    private static boolean isConfident(MatchDecision decision) {
        return decision == MatchDecision.EXACT_MATCH || decision == MatchDecision.HIGH_CONFIDENCE;
    }

    /**
     * El media-type del candidato debe concordar con el formato del anime:
     * formato MOVIE -> película, cualquier otro (TV/OVA/ONA/SPECIAL...) -> serie.
     * Sin esto, el matcher podía dar HIGH_CONFIDENCE a una película para un OVA y,
     * como {@code ProviderSyncService} enruta por {@code format=="MOVIE"}, ese OVA
     * acababa pegando a {@code /tv/{idPelícula}} -> 404 -> sin "dónde verlo". El
     * respaldo ya filtra por tipo (ver {@link #fallbackTmdbId}); aquí replicamos
     * esa garantía en la rama confiada. Si no concuerda, caemos al respaldo.
     */
    private static boolean mediaTypeMatchesFormat(TmdbTitle candidate, boolean movieFormat) {
        TmdbMediaType type = candidate.mediaType();
        return movieFormat ? type == TmdbMediaType.MOVIE : type == TmdbMediaType.TV;
    }

    private static String primaryQuery(Anime anime) {
        if (!isBlank(anime.getTitleEnglish())) {
            return anime.getTitleEnglish();
        }
        if (!isBlank(anime.getTitleRomaji())) {
            return anime.getTitleRomaji();
        }
        return anime.getTitleNative();
    }

    private static AniListAnime toAniListAnime(Anime anime) {
        return AniListAnime.builder()
                .id(anime.getAnilistId() != null ? anime.getAnilistId() : 0L)
                .romajiTitle(anime.getTitleRomaji())
                .englishTitle(anime.getTitleEnglish())
                .nativeTitle(anime.getTitleNative())
                .synonyms(List.copyOf(anime.getSynonyms()))
                .year(anime.getStartYear())
                .format(AnimeFormat.parse(anime.getFormat()))
                .episodes(anime.getEpisodes())
                .build();
    }

    private static TmdbTitle toTmdbTitle(TmdbSearchResult r) {
        return TmdbTitle.builder()
                .id(r.id())
                .title(r.displayName())
                .originalTitle(r.displayOriginalName())
                .year(parseYear(r.displayDate()))
                .mediaType(TmdbMediaType.fromTmdbString(r.mediaType()))
                .originalLanguage(r.originalLanguage())
                .popularity(r.popularity())
                .build();
    }

    /**
     * Heurística histórica de respaldo: entre los resultados de serie elige el
     * más popular, priorizando origen JP + año cercano (±1). La fecha importa
     * porque la popularidad de TMDb es muy dinámica: un spin-off recién
     * estrenado puede superar a la serie original (caso real "My Hero Academia:
     * Vigilantes" 2025 vs "My Hero Academia" 2016).
     */
    private static Long fallbackTmdbId(List<TmdbSearchResult> results, Integer animeYear, boolean preferMovie) {
        // Para anime de formato película el respaldo mira PELÍCULAS; para el
        // resto, series. Antes solo miraba series, así que una película que el
        // matcher no confirmaba caía a null o —peor— a una serie de nombre
        // parecido.
        List<TmdbSearchResult> pool = results.stream()
                .filter(preferMovie ? TmdbSearchResult::isMovie : TmdbSearchResult::isTv)
                .toList();
        if (pool.isEmpty()) {
            return null;
        }
        Comparator<TmdbSearchResult> byPopDesc = Comparator.comparingDouble(
                r -> Optional.ofNullable(r.popularity()).orElse(0.0));

        return pool.stream()
                .filter(r -> isJapanese(r, preferMovie))
                .filter(r -> yearMatches(r, animeYear))
                .max(byPopDesc)
                .or(() -> pool.stream()
                        .filter(r -> isJapanese(r, preferMovie))
                        .max(byPopDesc))
                .or(() -> pool.stream().max(byPopDesc))
                .map(TmdbSearchResult::id)
                .orElse(null);
    }

    private static boolean isMovieFormat(Anime anime) {
        return "MOVIE".equalsIgnoreCase(anime.getFormat());
    }

    private static boolean isJapanese(TmdbSearchResult r, boolean isMovie) {
        // Las películas de TMDb no traen origin_country (es un campo de TV); para
        // ellas el idioma original es la señal de origen japonés.
        if (isMovie) {
            return "ja".equalsIgnoreCase(r.originalLanguage());
        }
        return r.originCountry() != null && r.originCountry().contains("JP");
    }

    private static boolean yearMatches(TmdbSearchResult r, Integer animeYear) {
        Integer tmdbYear = parseYear(r.displayDate());
        return animeYear != null && tmdbYear != null && Math.abs(tmdbYear - animeYear) <= 1;
    }

    private static Integer parseYear(String date) {
        if (date == null || date.length() < 4) {
            return null;
        }
        try {
            return Integer.parseInt(date.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
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

    /** De dónde salió el tmdbId resuelto. */
    public enum MatchSource {
        MATCHER, FALLBACK, NONE, NO_RESULTS, NO_TITLE
    }

    /** Resultado de resolver un anime, sin persistir. */
    public record Resolution(Long tmdbId, MatchSource source, MatchDecision decision, double score) {
        static Resolution none(MatchSource source) {
            return new Resolution(null, source, MatchDecision.NO_MATCH, 0.0);
        }
    }

    /** Una diferencia detectada por el dry-run (tmdbId actual vs propuesto). */
    public record DryRunDiff(String slug, Long currentTmdbId, Long proposedTmdbId,
                             MatchSource source, MatchDecision decision, double score) {}

    /** Informe agregado del dry-run. {@code diffs} solo incluye los que cambian. */
    public record DryRunReport(int total, int changed, int unchanged, int nowMatched,
                               int nowUnmatched, int matcherWins, List<DryRunDiff> diffs) {
        public String summary() {
            return "total=" + total + " changed=" + changed + " unchanged=" + unchanged
                    + " nowMatched=" + nowMatched + " nowUnmatched=" + nowUnmatched
                    + " matcherWins=" + matcherWins;
        }
    }

    public record RematchResult(String slug, boolean matched) {}

    /** Un cambio que el re-match confiado aplicaría (o aplicó). */
    public record RematchConfidentChange(String slug, Long currentTmdbId, Long proposedTmdbId,
                                         MatchDecision decision, double score) {}

    /**
     * Informe del re-match confiado. {@code applied=false} = solo preview.
     * {@code changes} lista cada cambio (actual → propuesto) para revisarlo.
     */
    public record RematchConfidentReport(int total, boolean applied, int changed, int unchanged,
                                         int skipped, List<RematchConfidentChange> changes) {}
}
