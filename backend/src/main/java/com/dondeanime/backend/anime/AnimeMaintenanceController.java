package com.dondeanime.backend.anime;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dondeanime.backend.provider.ProviderSyncService;

@RestController
@RequestMapping("/api/anime")
public class AnimeMaintenanceController {

    private final AnimeSyncService syncService;
    private final AnimeMatchingService matchingService;
    private final ProviderSyncService providerSyncService;
    private final AnimeDescriptionEnricher descriptionEnricher;

    public AnimeMaintenanceController(
            AnimeSyncService syncService,
            AnimeMatchingService matchingService,
            ProviderSyncService providerSyncService,
            AnimeDescriptionEnricher descriptionEnricher) {
        this.syncService = syncService;
        this.matchingService = matchingService;
        this.providerSyncService = providerSyncService;
        this.descriptionEnricher = descriptionEnricher;
    }

    @PostMapping("/sync")
    public Map<String, Integer> sync(@RequestParam(defaultValue = "100") int count) {
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
}
