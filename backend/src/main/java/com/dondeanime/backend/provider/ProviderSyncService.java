package com.dondeanime.backend.provider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;
import com.dondeanime.backend.anime.tmdb.TmdbClient;
import com.dondeanime.backend.anime.tmdb.TmdbCountryProviders;
import com.dondeanime.backend.anime.tmdb.TmdbProvider;
import com.dondeanime.backend.anime.tmdb.TmdbProvidersResponse;
import com.dondeanime.backend.subscription.AlertService;

/**
 * Sincroniza la tabla watch_provider llamando a TMDb por cada anime
 * que ya tenga tmdbId asignado.
 *
 * Estrategia por anime: delete + insert atómicos via TransactionTemplate.
 * Borramos todos los WatchProvider del anime y reinsertamos los actuales.
 *
 * Por qué TransactionTemplate y no @Transactional: las llamadas a métodos
 * propios desde dentro de la misma clase NO pasan por el proxy de Spring,
 * con lo que la anotación se ignora silenciosamente. TransactionTemplate
 * funciona siempre porque es una API programática.
 *
 * Solo guardamos FLATRATE (incluido en suscripción) y FREE. RENT y BUY
 * los ignoramos: el objetivo es "dónde puedo VER el anime", no comprarlo.
 */
@Service
public class ProviderSyncService {

    private static final Logger log = LoggerFactory.getLogger(ProviderSyncService.class);

    private static final List<String> TARGET_COUNTRIES = List.of("ES", "MX", "AR", "CO", "CL");
    private static final long RATE_LIMIT_SLEEP_MS = 300;

    private final TmdbClient client;
    private final AnimeRepository animeRepository;
    private final WatchProviderRepository providerRepository;
    private final AlertService alertService;
    private final ApplicationEventPublisher eventPublisher;
    private final AvailabilityChangeService availabilityChangeService;
    private final TransactionTemplate transactionTemplate;

    public ProviderSyncService(
            TmdbClient client,
            AnimeRepository animeRepository,
            WatchProviderRepository providerRepository,
            AlertService alertService,
            ApplicationEventPublisher eventPublisher,
            AvailabilityChangeService availabilityChangeService,
            PlatformTransactionManager transactionManager) {
        this.client = client;
        this.animeRepository = animeRepository;
        this.providerRepository = providerRepository;
        this.alertService = alertService;
        this.eventPublisher = eventPublisher;
        this.availabilityChangeService = availabilityChangeService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public int syncAll() {
        List<Anime> animes = animeRepository.findAll();
        log.info("Sync providers iniciado: {} anime totales", animes.size());

        int processed = 0;
        int skipped = 0;
        int failed = 0;

        for (Anime a : animes) {
            if (a.getTmdbId() == null) {
                skipped++;
                continue;
            }
            try {
                Map<String, List<WatchProvider>> newProviders = syncOne(a);
                publishProviderAddedEvents(a, newProviders);
                processed++;
            } catch (Exception e) {
                log.error("Error sync providers slug={}: {}", a.getSlug(), e.getMessage());
                failed++;
            }
            sleep(RATE_LIMIT_SLEEP_MS);
        }

        log.info("Sync providers completado: {} procesados, {} sin tmdbId, {} fallos",
                processed, skipped, failed);
        return processed;
    }

    private void publishProviderAddedEvents(Anime anime, Map<String, List<WatchProvider>> providersByCountry) {
        if (providersByCountry == null || providersByCountry.isEmpty()) {
            return;
        }
        for (Map.Entry<String, List<WatchProvider>> entry : providersByCountry.entrySet()) {
            String countryCode = entry.getKey();
            List<WatchProvider> providers = entry.getValue();
            if (providers.isEmpty() || !alertService.hasPendingAlerts(anime.getId(), countryCode)) {
                continue;
            }
            eventPublisher.publishEvent(new ProviderAddedEvent(anime, countryCode, providers));
        }
    }

    private Map<String, List<WatchProvider>> syncOne(Anime anime) {
        TmdbProvidersResponse resp = client.getWatchProviders(anime.getTmdbId());
        if (resp == null || resp.results() == null) {
            return Map.of();
        }

        Instant now = Instant.now();
        List<WatchProvider> existing = providerRepository
                .findByAnimeIdOrderByCountryCodeAscProviderTypeAscProviderNameAsc(anime.getId());
        Set<ProviderKey> existingKeys = existing.stream()
                .map(ProviderKey::from)
                .collect(Collectors.toCollection(HashSet::new));
        List<WatchProvider> current = buildProviders(anime.getId(), resp, now);
        Map<String, List<WatchProvider>> newProvidersByCountry = current.stream()
                .filter(provider -> !existingKeys.contains(ProviderKey.from(provider)))
                .collect(Collectors.groupingBy(
                        WatchProvider::getCountryCode,
                        LinkedHashMap::new,
                        Collectors.toList()));

        transactionTemplate.executeWithoutResult(status -> {
            availabilityChangeService.recordChanges(anime, existing, current, now);
            providerRepository.deleteByAnimeId(anime.getId());
            current.forEach(providerRepository::save);
        });

        return newProvidersByCountry;
    }

    private List<WatchProvider> buildProviders(Long animeId, TmdbProvidersResponse response, Instant now) {
        List<WatchProvider> providers = new ArrayList<>();
        for (String country : TARGET_COUNTRIES) {
            TmdbCountryProviders cp = response.results().get(country);
            if (cp == null) continue;
            addProviders(providers, animeId, country, cp.flatrate(), "FLATRATE", now);
            addProviders(providers, animeId, country, cp.free(), "FREE", now);
        }
        return providers;
    }

    private void addProviders(
            List<WatchProvider> providers,
            Long animeId,
            String country,
            List<TmdbProvider> list,
            String type,
            Instant now) {
        if (list == null) {
            return;
        }
        for (TmdbProvider p : list) {
            WatchProvider wp = new WatchProvider();
            wp.setAnimeId(animeId);
            wp.setCountryCode(country);
            wp.setProviderName(p.providerName());
            wp.setProviderType(type);
            wp.setTmdbProviderId(p.providerId());
            wp.setLogoUrl(TmdbClient.fullLogoUrl(p.logoPath()));
            wp.setUpdatedAt(now);
            providers.add(wp);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record ProviderKey(String countryCode, String providerName, String providerType) {

        static ProviderKey from(WatchProvider provider) {
            return new ProviderKey(
                    provider.getCountryCode(),
                    provider.getProviderName(),
                    provider.getProviderType());
        }
    }
}
