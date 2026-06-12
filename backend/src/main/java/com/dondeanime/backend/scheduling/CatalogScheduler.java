package com.dondeanime.backend.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.dondeanime.backend.anime.AnimeDescriptionEnricher;
import com.dondeanime.backend.anime.AnimeMatchingService;
import com.dondeanime.backend.anime.AnimeSyncService;
import com.dondeanime.backend.anime.TrailerSyncService;
import com.dondeanime.backend.news.NewsIngestionResult;
import com.dondeanime.backend.news.NewsIngestionService;
import com.dondeanime.backend.news.NewsProcessingResult;
import com.dondeanime.backend.news.NewsProcessingService;
import com.dondeanime.backend.provider.ProviderSyncService;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

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
    private final MeterRegistry meterRegistry;
    private final ApplicationEventPublisher eventPublisher;
    private final String vercelDeployHook;
    private final NewsIngestionService newsIngestionService;
    private final boolean newsIngestionEnabled;
    private final NewsProcessingService newsProcessingService;
    private final boolean newsProcessingEnabled;

    public CatalogScheduler(
            AnimeSyncService syncService,
            AnimeMatchingService matchingService,
            AnimeDescriptionEnricher descriptionEnricher,
            ProviderSyncService providerSyncService,
            TrailerSyncService trailerSyncService,
            RestClient.Builder restClientBuilder,
            MeterRegistry meterRegistry,
            ApplicationEventPublisher eventPublisher,
            NewsIngestionService newsIngestionService,
            NewsProcessingService newsProcessingService,
            @Value("${vercel.deploy-hook:}") String vercelDeployHook,
            @Value("${news.ingestion.enabled:false}") boolean newsIngestionEnabled,
            @Value("${news.processing.enabled:false}") boolean newsProcessingEnabled) {
        this.syncService = syncService;
        this.matchingService = matchingService;
        this.descriptionEnricher = descriptionEnricher;
        this.providerSyncService = providerSyncService;
        this.trailerSyncService = trailerSyncService;
        this.restClient = restClientBuilder.build();
        this.meterRegistry = meterRegistry;
        this.eventPublisher = eventPublisher;
        this.newsIngestionService = newsIngestionService;
        this.newsProcessingService = newsProcessingService;
        this.vercelDeployHook = vercelDeployHook;
        this.newsIngestionEnabled = newsIngestionEnabled;
        this.newsProcessingEnabled = newsProcessingEnabled;
    }

    @Scheduled(cron = "${dondeanime.cron.sync-anilist:0 0 3,15 * * *}")
    public void syncAniList() {
        log.info("[scheduler] syncAniList: iniciando");
        boolean ok = false;
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            int n = syncService.syncPopular(AnimeSyncService.MAX_POPULAR_SYNC_COUNT);
            log.info("[scheduler] syncAniList: completado, {} anime", n);
            ok = true;
            recordSuccess("anilist", sample);
        } catch (Exception e) {
            log.error("[scheduler] syncAniList: ERROR", e);
            recordError("anilist", sample);
            publishJobFailure("anilist", e);
        }
        if (ok) {
            triggerVercelRebuild();
        }
    }

    @Scheduled(cron = "${dondeanime.cron.match-tmdb:0 0 4 * * *}")
    public void matchTmdb() {
        log.info("[scheduler] matchTmdb: iniciando");
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            int n = matchingService.matchAll();
            log.info("[scheduler] matchTmdb: completado, {} match nuevos", n);
            int enriched = descriptionEnricher.enrichMissingSpanishDescriptions();
            log.info("[scheduler] matchTmdb: {} descripciones es-ES nuevas", enriched);
            recordSuccess("match", sample);
        } catch (Exception e) {
            log.error("[scheduler] matchTmdb: ERROR", e);
            recordError("match", sample);
            publishJobFailure("match", e);
        }
    }

    @Scheduled(cron = "${dondeanime.cron.sync-providers:0 0 5 * * *}")
    public void syncProviders() {
        log.info("[scheduler] syncProviders: iniciando");
        Timer.Sample sample = Timer.start(meterRegistry);
        boolean ok = false;
        try {
            int providers = providerSyncService.syncAll();
            int trailers = trailerSyncService.syncAll();
            log.info("[scheduler] syncProviders: completado, {} providers procesados, {} trailers procesados",
                    providers, trailers);
            ok = true;
            recordSuccess("providers", sample);
        } catch (Exception e) {
            log.error("[scheduler] syncProviders: ERROR", e);
            recordError("providers", sample);
            publishJobFailure("providers", e);
        }
        if (ok) {
            triggerVercelRebuild();
        }
    }

    /**
     * Ingesta noticias de los feeds RSS. Independiente del resto: aunque el
     * scheduler esté activo, esto solo corre si news.ingestion.enabled=true,
     * para poder encenderlo por separado cuando el News Engine esté listo.
     */
    @Scheduled(cron = "${dondeanime.cron.ingest-news:0 30 */6 * * *}")
    public void ingestNews() {
        if (!newsIngestionEnabled) {
            log.debug("[scheduler] ingestNews: desactivado (news.ingestion.enabled=false)");
            return;
        }
        log.info("[scheduler] ingestNews: iniciando");
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            NewsIngestionResult result = newsIngestionService.ingestAll();
            if (result.itemsErrored() > 0) {
                // Fallos reales de guardado: que la métrica no lo confunda con "sin novedades".
                log.warn("[scheduler] ingestNews: completado con {} errores de guardado ({} nuevas, {} fuentes)",
                        result.itemsErrored(), result.itemsCreated(), result.sourcesProcessed());
                recordError("news", sample);
            } else {
                log.info("[scheduler] ingestNews: completado, {} nuevas de {} fuentes",
                        result.itemsCreated(), result.sourcesProcessed());
                recordSuccess("news", sample);
            }
        } catch (Exception e) {
            log.error("[scheduler] ingestNews: ERROR", e);
            recordError("news", sample);
            publishJobFailure("news", e);
        }
    }

    /**
     * Procesa los borradores que dejó la ingesta (resumen, meta, match de anime
     * y publicación si news.processing.publish=true). Corre media hora después
     * de cada ingesta para no dejar borradores acumulados hasta el siguiente
     * ciclo. Mismo patrón que ingestNews: flag propio, solo corre si
     * news.processing.enabled=true.
     */
    @Scheduled(cron = "${dondeanime.cron.process-news:0 0 1/6 * * *}")
    public void processNews() {
        if (!newsProcessingEnabled) {
            log.debug("[scheduler] processNews: desactivado (news.processing.enabled=false)");
            return;
        }
        log.info("[scheduler] processNews: iniciando");
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            NewsProcessingResult result = newsProcessingService.processDrafts();
            log.info("[scheduler] processNews: completado, {} procesadas ({} via LLM, {} fallos LLM), "
                            + "{} publicadas, {} a revision, {} con anime de {} borradores",
                    result.itemsProcessed(), result.llmProcessed(), result.llmFailed(),
                    result.itemsPublished(), result.sentToReview(), result.animeMatched(), result.draftsSeen());
            recordSuccess("news-processing", sample);
        } catch (Exception e) {
            log.error("[scheduler] processNews: ERROR", e);
            recordError("news-processing", sample);
            publishJobFailure("news-processing", e);
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

    private void recordSuccess(String job, Timer.Sample sample) {
        meterRegistry.counter("dondeanime.scheduler." + job + ".success.count").increment();
        sample.stop(meterRegistry.timer("dondeanime.scheduler." + job + ".duration"));
    }

    private void recordError(String job, Timer.Sample sample) {
        meterRegistry.counter("dondeanime.scheduler." + job + ".error.count").increment();
        sample.stop(meterRegistry.timer("dondeanime.scheduler." + job + ".duration"));
    }

    private void publishJobFailure(String job, Exception error) {
        try {
            eventPublisher.publishEvent(new SchedulerJobFailedEvent(job, error));
        } catch (RuntimeException alertError) {
            log.error("[scheduler] no se pudo publicar alerta de fallo para '{}'", job, alertError);
        }
    }
}
