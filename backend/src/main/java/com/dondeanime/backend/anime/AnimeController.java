package com.dondeanime.backend.anime;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.dondeanime.backend.affiliate.AffiliateLinkService;
import com.dondeanime.backend.provider.ProviderDto;
import com.dondeanime.backend.provider.ProviderSyncService;
import com.dondeanime.backend.provider.WatchProviderRepository;

@RestController
@RequestMapping("/api/anime")
public class AnimeController {

    private final AnimeRepository repository;
    private final AnimeSyncService syncService;
    private final AnimeMatchingService matchingService;
    private final ProviderSyncService providerSyncService;
    private final AnimeDescriptionEnricher descriptionEnricher;
    private final TrailerSyncService trailerSyncService;
    private final WatchProviderRepository providerRepository;
    private final AnimeOverrideService overrideService;
    private final AffiliateLinkService affiliateLinkService;

    public AnimeController(
            AnimeRepository repository,
            AnimeSyncService syncService,
            AnimeMatchingService matchingService,
            ProviderSyncService providerSyncService,
            AnimeDescriptionEnricher descriptionEnricher,
            TrailerSyncService trailerSyncService,
            WatchProviderRepository providerRepository,
            AnimeOverrideService overrideService,
            AffiliateLinkService affiliateLinkService) {
        this.repository = repository;
        this.syncService = syncService;
        this.matchingService = matchingService;
        this.providerSyncService = providerSyncService;
        this.descriptionEnricher = descriptionEnricher;
        this.trailerSyncService = trailerSyncService;
        this.providerRepository = providerRepository;
        this.overrideService = overrideService;
        this.affiliateLinkService = affiliateLinkService;
    }

    @GetMapping
    public List<AnimeSummaryDto> getAll() {
        return repository.findAll().stream()
                .map(AnimeSummaryDto::from)
                .toList();
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<UpcomingAnimeDto>> upcoming(@RequestParam(defaultValue = "7") int days) {
        if (days < 1 || days > 365) {
            return ResponseEntity.badRequest().build();
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate until = today.plusDays(days);

        List<UpcomingAnimeDto> upcoming = repository.findAll().stream()
                .map(AnimeController::withStartDate)
                .flatMap(Optional::stream)
                .filter(item -> !item.startDate().isBefore(today) && !item.startDate().isAfter(until))
                .sorted(Comparator
                        .comparing(AnimeWithStartDate::startDate)
                        .thenComparing(
                                item -> popularityOrZero(item.anime()),
                                Comparator.reverseOrder())
                        .thenComparing(item -> titleOrSlug(item.anime())))
                .map(item -> UpcomingAnimeDto.from(item.anime()))
                .toList();

        return ResponseEntity.ok(upcoming);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<AnimeDetailResponse> getBySlug(@PathVariable String slug) {
        return repository.findBySlugWithCharacters(slug)
                .map(anime -> {
                    Map<String, List<ProviderDto>> byCountry = providerRepository
                            .findByAnimeIdOrderByCountryCodeAscProviderTypeAscProviderNameAsc(anime.getId())
                            .stream()
                            .map(affiliateLinkService::toProviderDto)
                            .collect(Collectors.groupingBy(
                                    ProviderDto::countryCode,
                                    LinkedHashMap::new,
                                    Collectors.toList()));
                    return ResponseEntity.ok(new AnimeDetailResponse(
                            AnimeDetailDto.from(anime, overrideService.findSpanishOverrides(anime)), byCountry));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
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

    private static Optional<AnimeWithStartDate> withStartDate(Anime anime) {
        if (anime.getStartYear() == null || anime.getStartMonth() == null || anime.getStartDay() == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new AnimeWithStartDate(
                    anime,
                    LocalDate.of(anime.getStartYear(), anime.getStartMonth(), anime.getStartDay())));
        } catch (DateTimeException e) {
            return Optional.empty();
        }
    }

    private static int popularityOrZero(Anime anime) {
        return anime.getPopularity() == null ? 0 : anime.getPopularity();
    }

    private static String titleOrSlug(Anime anime) {
        if (anime.getTitleEnglish() != null && !anime.getTitleEnglish().isBlank()) {
            return anime.getTitleEnglish();
        }
        if (anime.getTitleRomaji() != null && !anime.getTitleRomaji().isBlank()) {
            return anime.getTitleRomaji();
        }
        return anime.getSlug() == null ? "" : anime.getSlug();
    }

    private record AnimeWithStartDate(Anime anime, LocalDate startDate) {}
}
