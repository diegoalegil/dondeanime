package com.dondeanime.backend.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.dondeanime.backend.anime.AnimeMatchingService;
import com.dondeanime.backend.anime.AnimeSyncService;
import com.dondeanime.backend.provider.ProviderSyncService;

/**
 * Jobs programados que mantienen el catálogo al día sin intervención manual.
 *
 * Solo se activa si scheduling.enabled=true. En local lo dejamos a
 * false por defecto: no queremos que dispare syncs mientras desarrollamos.
 * En producción (Hetzner) se activará con scheduling.enabled=true.
 *
 * Cron expressions configurables vía properties para ajustar sin
 * recompilar. Defaults espaciados para no solapar:
 *   - sync-anilist:    cada 12h, 3am y 3pm
 *   - match-tmdb:      cada 24h, 4am
 *   - sync-providers:  cada 24h, 5am
 *
 * Cron de Spring tiene 6 campos (seg min hora día mes día_semana).
 * Distinto del cron Unix de 5 campos.
 */
@Component
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true")
public class CatalogScheduler {

    private static final Logger log = LoggerFactory.getLogger(CatalogScheduler.class);

    private final AnimeSyncService syncService;
    private final AnimeMatchingService matchingService;
    private final ProviderSyncService providerSyncService;

    public CatalogScheduler(
            AnimeSyncService syncService,
            AnimeMatchingService matchingService,
            ProviderSyncService providerSyncService) {
        this.syncService = syncService;
        this.matchingService = matchingService;
        this.providerSyncService = providerSyncService;
    }

    @Scheduled(cron = "${dondeanime.cron.sync-anilist:0 0 3,15 * * *}")
    public void syncAniList() {
        log.info("[scheduler] syncAniList: iniciando");
        try {
            int n = syncService.syncPopular(100);
            log.info("[scheduler] syncAniList: completado, {} anime", n);
        } catch (Exception e) {
            log.error("[scheduler] syncAniList: ERROR", e);
        }
    }

    @Scheduled(cron = "${dondeanime.cron.match-tmdb:0 0 4 * * *}")
    public void matchTmdb() {
        log.info("[scheduler] matchTmdb: iniciando");
        try {
            int n = matchingService.matchAll();
            log.info("[scheduler] matchTmdb: completado, {} match nuevos", n);
        } catch (Exception e) {
            log.error("[scheduler] matchTmdb: ERROR", e);
        }
    }

    @Scheduled(cron = "${dondeanime.cron.sync-providers:0 0 5 * * *}")
    public void syncProviders() {
        log.info("[scheduler] syncProviders: iniciando");
        try {
            int n = providerSyncService.syncAll();
            log.info("[scheduler] syncProviders: completado, {} procesados", n);
        } catch (Exception e) {
            log.error("[scheduler] syncProviders: ERROR", e);
        }
    }
}
