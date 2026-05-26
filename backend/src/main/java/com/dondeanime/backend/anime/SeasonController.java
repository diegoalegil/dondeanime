package com.dondeanime.backend.anime;

import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping({"/api/seasons", "/api/v1/seasons"})
@Tag(name = "Seasons", description = "Temporadas de estreno cubiertas por el catalogo")
public class SeasonController {

    /** Seasons válidas en AniList. Anti-fat-fingers en el path param. */
    private static final Set<String> VALID_SEASONS = Set.of("WINTER", "SPRING", "SUMMER", "FALL");

    private final AnimeRepository animeRepository;

    public SeasonController(AnimeRepository animeRepository) {
        this.animeRepository = animeRepository;
    }

    /**
     * Todas las temporadas que tenemos cubiertas, ordenadas de más
     * reciente a más antigua.
     */
    @GetMapping
    @Operation(summary = "Lista temporadas", description = "Devuelve temporadas agregadas de mas reciente a mas antigua.")
    public List<SeasonSummaryDto> list() {
        return animeRepository.aggregateSeasons().stream()
                .map(r -> new SeasonSummaryDto(r.getYear(), r.getSeason(), r.getAnimeCount()))
                .toList();
    }

    /**
     * Anime de una temporada concreta.
     * Ejemplo: GET /api/seasons/2024/winter
     * 400 si season no es válida.
     */
    @GetMapping("/{year}/{season}")
    @Operation(summary = "Lista anime por temporada", description = "Devuelve anime de una temporada concreta de AniList.")
    public ResponseEntity<List<AnimeSummaryDto>> animesBySeason(
            @PathVariable int year,
            @PathVariable String season) {
        String normalized = season.toUpperCase();
        if (!VALID_SEASONS.contains(normalized)) {
            return ResponseEntity.badRequest().build();
        }
        List<AnimeSummaryDto> list = animeRepository
                .findBySeasonYearAndSeason(year, normalized)
                .stream()
                .map(AnimeSummaryDto::from)
                .toList();
        return ResponseEntity.ok(list);
    }
}
