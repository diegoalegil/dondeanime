package com.dondeanime.backend.anime;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dondeanime.backend.affiliate.AffiliateLinkService;
import com.dondeanime.backend.provider.ProviderDto;
import com.dondeanime.backend.provider.WatchProviderRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping({"/api/anime", "/api/v1/anime"})
@Tag(name = "Anime", description = "Catalogo publico de anime")
public class AnimeController {

    private final AnimeRepository repository;
    private final WatchProviderRepository providerRepository;
    private final AnimeOverrideService overrideService;
    private final AffiliateLinkService affiliateLinkService;
    private final RecommendationService recommendationService;

    public AnimeController(
            AnimeRepository repository,
            WatchProviderRepository providerRepository,
            AnimeOverrideService overrideService,
            AffiliateLinkService affiliateLinkService,
            RecommendationService recommendationService) {
        this.repository = repository;
        this.providerRepository = providerRepository;
        this.overrideService = overrideService;
        this.affiliateLinkService = affiliateLinkService;
        this.recommendationService = recommendationService;
    }

    @GetMapping
    @Operation(summary = "Lista todos los anime", description = "Devuelve la vista resumida del catalogo ordenada como esta almacenada.")
    public List<AnimeSummaryDto> getAll() {
        return repository.findAllWithGenres().stream()
                .map(AnimeSummaryDto::from)
                .toList();
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Lista proximos estrenos", description = "Devuelve anime con fecha completa dentro del rango indicado.")
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

    @GetMapping("/{slug}/similar")
    public ResponseEntity<List<AnimeSummaryDto>> getSimilar(@PathVariable String slug) {
        return repository.findBySlug(slug)
                .map(anime -> ResponseEntity.ok(
                        recommendationService.findSimilar(anime.getId(), 10).stream()
                                .map(AnimeSummaryDto::from)
                                .toList()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Obtiene el detalle de un anime", description = "Devuelve la ficha publica y providers agrupados por pais.")
    public ResponseEntity<AnimeDetailResponse> getBySlug(@PathVariable String slug) {
        return repository.findBySlug(slug)
                .map(anime -> {
                    var byCountry = providerRepository
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
