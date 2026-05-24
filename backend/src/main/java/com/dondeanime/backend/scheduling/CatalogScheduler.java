package com.dondeanime.backend.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.dondeanime.backend.anime.AnimeMatchingService;
import com.dondeanime.backend.anime.AnimeSyncService;
import com.dondeanime.backend.provider.ProviderSyncService;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

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
 *
 * Tras completar syncProviders (último paso del pipeline) dispara
 * un Deploy Hook de Vercel para que el frontend rebuildee con datos
 * frescos. Solo si vercel.deploy-hook está configurado (env var
 * VERCEL_DEPLOY_HOOK en .env.prod del VPS).
 */
@Component
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true")
public class CatalogScheduler {

    private static final Logger log = LoggerFactory.getLogger(CatalogScheduler.class);

    private final AnimeSyncService syncService;
    private final AnimeMatchingService matchingService;
    private final ProviderSyncService providerSyncService;
    private final RestClient restClient;
    private final MeterRegistry meterRegistry;
    private final ApplicationEventPublisher eventPublisher;
    private final String vercelDeployHook;

    public CatalogScheduler(
            AnimeSyncService syncService,
            AnimeMatchingService matchingService,
            ProviderSyncService providerSyncService,
            RestClient.Builder restClientBuilder,
            MeterRegistry meterRegistry,
            ApplicationEventPublisher eventPublisher,
            @Value("${vercel.deploy-hook:}") String vercelDeployHook) {
        this.syncService = syncService;
        this.matchingService = matchingService;
        this.providerSyncService = providerSyncService;
        this.restClient = restClientBuilder.build();
        this.meterRegistry = meterRegistry;
        this.eventPublisher = eventPublisher;
        this.vercelDeployHook = vercelDeployHook;
    }

    @Scheduled(cron = "${dondeanime.cron.sync-anilist:0 0 3,15 * * *}")
    public void syncAniList() {
        log.info("[scheduler] syncAniList: iniciando");
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            int n = syncService.syncPopular(100);
            log.info("[scheduler] syncAniList: completado, {} anime", n);
            recordSuccess("anilist", sample);
        } catch (Exception e) {
            log.error("[scheduler] syncAniList: ERROR", e);
            recordError("anilist", sample);
            publishJobFailure("anilist", e);
        }
    }

    @Scheduled(cron = "${dondeanime.cron.match-tmdb:0 0 4 * * *}")
    public void matchTmdb() {
        log.info("[scheduler] matchTmdb: iniciando");
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            int n = matchingService.matchAll();
            log.info("[scheduler] matchTmdb: completado, {} match nuevos", n);
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
            int n = providerSyncService.syncAll();
            log.info("[scheduler] syncProviders: completado, {} procesados", n);
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
     * Llama al Deploy Hook de Vercel para forzar rebuild del frontend
     * con los datos frescos. Si vercel.deploy-hook no está configurado
     * (string vacío) es no-op.
     *
     * El hook devuelve 201 Created con un JSON tipo
     *   {"job":{"id":"...","state":"PENDING","createdAt":...}}
     * No esperamos a que el build termine; Vercel lo procesa async.
     */
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
