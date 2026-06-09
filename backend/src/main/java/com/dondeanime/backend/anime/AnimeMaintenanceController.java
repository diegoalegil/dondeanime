package com.dondeanime.backend.anime;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.dondeanime.backend.provider.ProviderSyncService;

import io.swagger.v3.oas.annotations.Hidden;

@Hidden
@RestController
@RequestMapping("/api/anime")
public class AnimeMaintenanceController {

    private final AnimeSyncService syncService;
    private final AnimeMatchingService matchingService;
    private final ProviderSyncService providerSyncService;
    private final AnimeDescriptionEnricher descriptionEnricher;
    private final TrailerSyncService trailerSyncService;

    public AnimeMaintenanceController(
            AnimeSyncService syncService,
            AnimeMatchingService matchingService,
            ProviderSyncService providerSyncService,
            AnimeDescriptionEnricher descriptionEnricher,
            TrailerSyncService trailerSyncService) {
        this.syncService = syncService;
        this.matchingService = matchingService;
        this.providerSyncService = providerSyncService;
        this.descriptionEnricher = descriptionEnricher;
        this.trailerSyncService = trailerSyncService;
    }

    @PostMapping("/sync")
    public Map<String, Integer> sync(@RequestParam(defaultValue = "100") int count) {
        if (count < 1 || count > AnimeSyncService.MAX_POPULAR_SYNC_COUNT) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "count debe estar entre 1 y " + AnimeSyncService.MAX_POPULAR_SYNC_COUNT);
        }
        int synced = syncService.syncPopular(count);
        return Map.of("synced", synced);
    }

    @PostMapping("/match")
    public Map<String, Integer> match() {
        int matched = matchingService.matchAll();
        int descriptionsEnriched = descriptionEnricher.enrichMissingSpanishDescriptions();
        return Map.of("matched", matched, "descriptionsEnriched", descriptionsEnriched);
    }

    @PostMapping("/sync-providers")
    public Map<String, Integer> syncProviders() {
        int processed = providerSyncService.syncAll();
        return Map.of("processed", processed);
    }

    @PostMapping("/sync-trailers")
    public Map<String, Integer> syncTrailers() {
        int processed = trailerSyncService.syncAll();
        return Map.of("processed", processed);
    }
}
