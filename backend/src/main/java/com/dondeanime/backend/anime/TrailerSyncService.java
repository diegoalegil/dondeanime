package com.dondeanime.backend.anime;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dondeanime.backend.anime.tmdb.TmdbClient;
import com.dondeanime.backend.anime.tmdb.TmdbVideo;
import com.dondeanime.backend.anime.tmdb.TmdbVideosResponse;

/**
 * Sincroniza el primer trailer oficial disponible en TMDb.
 *
 * Guardamos el key de YouTube en anime.trailerYoutubeId. El frontend
 * compone la URL embed, así no persistimos URLs derivadas.
 */
@Service
public class TrailerSyncService {

    private static final Logger log = LoggerFactory.getLogger(TrailerSyncService.class);
    private static final long RATE_LIMIT_SLEEP_MS = 300;

    private final TmdbClient client;
    private final AnimeRepository repository;

    public TrailerSyncService(TmdbClient client, AnimeRepository repository) {
        this.client = client;
        this.repository = repository;
    }

    public int syncAll() {
        List<Anime> animes = repository.findAll();
        log.info("Sync trailers iniciado: {} anime totales", animes.size());

        int processed = 0;
        int skipped = 0;
        int failed = 0;
        int changed = 0;

        for (Anime anime : animes) {
            if (anime.getTmdbId() == null) {
                skipped++;
                continue;
            }
            try {
                String trailerYoutubeId = findFirstYoutubeTrailerId(anime.getTmdbId());
                if (!Objects.equals(anime.getTrailerYoutubeId(), trailerYoutubeId)) {
                    anime.setTrailerYoutubeId(trailerYoutubeId);
                    repository.save(anime);
                    changed++;
                }
                processed++;
            } catch (Exception e) {
                log.error("Error sync trailer slug={}: {}", anime.getSlug(), e.getMessage());
                failed++;
            }
            sleep(RATE_LIMIT_SLEEP_MS);
        }

        log.info("Sync trailers completado: {} procesados, {} cambiados, {} sin tmdbId, {} fallos",
                processed, changed, skipped, failed);
        return processed;
    }

    private String findFirstYoutubeTrailerId(Long tmdbId) {
        TmdbVideosResponse response = client.getTrailers(tmdbId, "es");
        if (response == null || response.results() == null) {
            return null;
        }
        return response.results().stream()
                .filter(TrailerSyncService::isYoutubeTrailer)
                .map(TmdbVideo::key)
                .filter(key -> key != null && !key.isBlank())
                .findFirst()
                .orElse(null);
    }

    private static boolean isYoutubeTrailer(TmdbVideo video) {
        return video != null
                && "YouTube".equalsIgnoreCase(video.site())
                && "Trailer".equalsIgnoreCase(video.type());
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
