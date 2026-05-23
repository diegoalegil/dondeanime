package com.dondeanime.backend.anime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dondeanime.backend.provider.ProviderSyncService;
import com.dondeanime.backend.provider.WatchProvider;
import com.dondeanime.backend.provider.WatchProviderRepository;

@RestController
@RequestMapping("/api/anime")
public class AnimeController {

    private final AnimeRepository repository;
    private final AnimeSyncService syncService;
    private final AnimeMatchingService matchingService;
    private final ProviderSyncService providerSyncService;
    private final WatchProviderRepository providerRepository;

    public AnimeController(
            AnimeRepository repository,
            AnimeSyncService syncService,
            AnimeMatchingService matchingService,
            ProviderSyncService providerSyncService,
            WatchProviderRepository providerRepository) {
        this.repository = repository;
        this.syncService = syncService;
        this.matchingService = matchingService;
        this.providerSyncService = providerSyncService;
        this.providerRepository = providerRepository;
    }

    @GetMapping
    public List<Anime> getAll() {
        return repository.findAll();
    }

    /**
     * Detalle de un anime + sus watch providers agrupados por país.
     * 404 si el slug no existe.
     */
    @GetMapping("/{slug}")
    public ResponseEntity<AnimeDetailResponse> getBySlug(@PathVariable String slug) {
        return repository.findBySlug(slug)
                .map(anime -> {
                    List<WatchProvider> providers = providerRepository
                            .findByAnimeIdOrderByCountryCodeAscProviderTypeAscProviderNameAsc(anime.getId());
                    Map<String, List<WatchProvider>> byCountry = providers.stream()
                            .collect(Collectors.groupingBy(
                                    WatchProvider::getCountryCode,
                                    LinkedHashMap::new,
                                    Collectors.toList()));
                    return ResponseEntity.ok(new AnimeDetailResponse(anime, byCountry));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Dispara el sync de AniList. Manual durante desarrollo; en semana 4
     * lo automatizaremos con @Scheduled cada 12h.
     *
     * Ejemplo: POST /api/anime/sync?count=100
     */
    @PostMapping("/sync")
    public Map<String, Integer> sync(@RequestParam(defaultValue = "100") int count) {
        int synced = syncService.syncPopular(count);
        return Map.of("synced", synced);
    }

    /**
     * Cruza cada anime con su id de TMDb. Skip si ya está matcheado.
     * Tarda ~30s para 100 anime (300ms de sleep entre requests).
     */
    @PostMapping("/match")
    public Map<String, Integer> match() {
        int matched = matchingService.matchAll();
        return Map.of("matched", matched);
    }

    /**
     * Sincroniza watch providers desde TMDb para cada anime con tmdbId.
     * Tarda ~30s para 100 anime.
     */
    @PostMapping("/sync-providers")
    public Map<String, Integer> syncProviders() {
        int processed = providerSyncService.syncAll();
        return Map.of("processed", processed);
    }
}
