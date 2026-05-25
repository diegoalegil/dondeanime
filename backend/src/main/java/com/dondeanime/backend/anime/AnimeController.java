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
    private final WatchProviderRepository providerRepository;
    private final AnimeOverrideService overrideService;
    private final AffiliateLinkService affiliateLinkService;

    public AnimeController(
            AnimeRepository repository,
            AnimeSyncService syncService,
            AnimeMatchingService matchingService,
            ProviderSyncService providerSyncService,
            AnimeDescriptionEnricher descriptionEnricher,
            WatchProviderRepository providerRepository,
            AnimeOverrideService overrideService,
            AffiliateLinkService affiliateLinkService) {
        this.repository = repository;
        this.syncService = syncService;
        this.matchingService = matchingService;
        this.providerSyncService = providerSyncService;
        this.descriptionEnricher = descriptionEnricher;
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

    /**
     * Próximos estrenos con fecha completa, ordenados por startDate asc.
     * Solo incluye anime con año, mes y día: si AniList devuelve fecha
     * parcial no podemos prometer "próxima semana" con precisión.
     */
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

    /**
     * Detalle de un anime + sus watch providers agrupados por país.
     * 404 si el slug no existe.
     */
    @GetMapping("/{slug}")
    public ResponseEntity<AnimeDetailResponse> getBySlug(@PathVariable String slug) {
        return repository.findBySlugWithStudios(slug)
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

    /**
     * Dispara el sync de AniList. Manual durante desarrollo; el scheduler
     * lo ejecuta cada 12h en producción.
     *
     * Ejemplo: POST /api/anime/sync?count=500
     */
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

    /**
     * Cruza cada anime con su id de TMDb. Skip si ya está matcheado.
     * Tarda ~30s para 100 anime, ~2.5min para 500 (300ms entre requests).
     */
    @PostMapping("/match")
    public Map<String, Integer> match() {
        int matched = matchingService.matchAll();
        int descriptionsEnriched = descriptionEnricher.enrichMissingSpanishDescriptions();
        return Map.of("matched", matched, "descriptionsEnriched", descriptionsEnriched);
    }

    /**
     * Sincroniza watch providers desde TMDb para cada anime con tmdbId.
     * Tarda ~30s para 100 anime, ~2.5min para 500.
     */
    @PostMapping("/sync-providers")
    public Map<String, Integer> syncProviders() {
        int processed = providerSyncService.syncAll();
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
