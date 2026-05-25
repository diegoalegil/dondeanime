package com.dondeanime.backend.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.dondeanime.backend.anime.AnimeDescriptionEnricher;
import com.dondeanime.backend.anime.AnimeMatchingService;
import com.dondeanime.backend.anime.AnimeSyncService;
import com.dondeanime.backend.anime.TrailerSyncService;
import com.dondeanime.backend.provider.ProviderSyncService;

/**
 * Jobs programados que mantienen el catalogo al dia sin intervencion manual.
 *
 * Solo se activa si scheduling.enabled=true. En local lo dejamos a false
 * por defecto: no queremos que dispare syncs mientras desarrollamos.
 */
@Component
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true")
public class CatalogScheduler {

    private static final Logger log = LoggerFactory.getLogger(CatalogScheduler.class);

    private final AnimeSyncService syncService;
    private final AnimeMatchingService matchingService;
    private final AnimeDescriptionEnricher descriptionEnricher;
    private final ProviderSyncService providerSyncService;
    private final TrailerSyncService trailerSyncService;
    private final RestClient restClient;
    private final String vercelDeployHook;

    public CatalogScheduler(
            AnimeSyncService syncService,
            AnimeMatchingService matchingService,
            AnimeDescriptionEnricher descriptionEnricher,
            ProviderSyncService providerSyncService,
            TrailerSyncService trailerSyncService,
            RestClient.Builder restClientBuilder,
            @Value("${vercel.deploy-hook:}") String vercelDeployHook) {
        this.syncService = syncService;
        this.matchingService = matchingService;
        this.descriptionEnricher = descriptionEnricher;
        this.providerSyncService = providerSyncService;
        this.trailerSyncService = trailerSyncService;
        this.restClient = restClientBuilder.build();
        this.vercelDeployHook = vercelDeployHook;
    }

    @Scheduled(cron = "${dondeanime.cron.sync-anilist:0 0 3,15 * * *}")
    public void syncAniList() {
        log.info("[scheduler] syncAniList: iniciando");
        boolean ok = false;
        try {
            int n = syncService.syncPopular(AnimeSyncService.MAX_POPULAR_SYNC_COUNT);
            log.info("[scheduler] syncAniList: completado, {} anime", n);
            ok = true;
        } catch (Exception e) {
            log.error("[scheduler] syncAniList: ERROR", e);
        }
        if (ok) {
            triggerVercelRebuild();
        }
    }

    @Scheduled(cron = "${dondeanime.cron.match-tmdb:0 0 4 * * *}")
    public void matchTmdb() {
        log.info("[scheduler] matchTmdb: iniciando");
        try {
            int n = matchingService.matchAll();
            log.info("[scheduler] matchTmdb: completado, {} match nuevos", n);
            int enriched = descriptionEnricher.enrichMissingSpanishDescriptions();
            log.info("[scheduler] matchTmdb: {} descripciones es-ES nuevas", enriched);
        } catch (Exception e) {
            log.error("[scheduler] matchTmdb: ERROR", e);
        }
    }

    @Scheduled(cron = "${dondeanime.cron.sync-providers:0 0 5 * * *}")
    public void syncProviders() {
        log.info("[scheduler] syncProviders: iniciando");
        boolean ok = false;
        try {
            int providers = providerSyncService.syncAll();
            int trailers = trailerSyncService.syncAll();
            log.info("[scheduler] syncProviders: completado, {} providers procesados, {} trailers procesados",
                    providers, trailers);
            ok = true;
        } catch (Exception e) {
            log.error("[scheduler] syncProviders: ERROR", e);
        }
        if (ok) {
            triggerVercelRebuild();
        }
    }

    private void triggerVercelRebuild() {
        if (vercelDeployHook == null || vercelDeployHook.isBlank()) {
            log.debug("[scheduler] Vercel deploy hook no configurado, no se dispara rebuild");
            return;
        }
        try {
            restClient.post()
                    .uri(vercelDeployHook)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[scheduler] Vercel deploy hook disparado, rebuild en marcha");
        } catch (Exception e) {
            log.error("[scheduler] Error disparando Vercel deploy hook: {}", e.getMessage());
        }
    }
}
