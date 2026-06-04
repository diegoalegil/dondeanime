package com.dondeanime.backend.admin;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dondeanime.backend.anime.AnimeDetailDto;
import com.dondeanime.backend.anime.AnimeMatchingService;
import com.dondeanime.backend.anime.AnimeOverrideService;
import com.dondeanime.backend.anime.AnimeRepository;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/admin/anime")
public class AnimeAdminController {

    private final AnimeRepository animeRepository;
    private final AnimeOverrideService overrideService;
    private final AnimeMatchingService matchingService;

    public AnimeAdminController(
            AnimeRepository animeRepository,
            AnimeOverrideService overrideService,
            AnimeMatchingService matchingService) {
        this.animeRepository = animeRepository;
        this.overrideService = overrideService;
        this.matchingService = matchingService;
    }

    @PostMapping("/{slug}/override")
    public ResponseEntity<AnimeDetailDto> saveOverride(
            @PathVariable String slug,
            @Valid @RequestBody AnimeOverrideRequest request,
            Principal principal) {
        return animeRepository.findBySlugWithCharacters(slug)
                .map(anime -> {
                    overrideService.saveOverride(
                            anime,
                            request.fieldName(),
                            request.fieldValue(),
                            request.locale(),
                            principal.getName());
                    return ResponseEntity.ok(AnimeDetailDto.from(
                            anime,
                            overrideService.findSpanishOverrides(anime)));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{slug}/override")
    public ResponseEntity<AnimeDetailDto> deleteOverride(
            @PathVariable String slug,
            @RequestParam(name = "field") String fieldName,
            @RequestParam(defaultValue = AnimeOverrideService.DEFAULT_LOCALE) String locale) {
        return animeRepository.findBySlugWithCharacters(slug)
                .map(anime -> {
                    overrideService.deleteOverride(anime, fieldName, locale);
                    return ResponseEntity.ok(AnimeDetailDto.from(
                            anime,
                            overrideService.findSpanishOverrides(anime)));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{slug}/overrides")
    public ResponseEntity<List<AnimeOverrideDto>> getOverrides(@PathVariable String slug) {
        return animeRepository.findBySlug(slug)
                .map(anime -> {
                    List<AnimeOverrideDto> overrides = overrideService.listOverrides(anime).stream()
                            .map(override -> AnimeOverrideDto.from(
                                    override,
                                    overrideService.originalValue(anime, override.getFieldName())))
                            .toList();
                    return ResponseEntity.ok(overrides);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{slug}/rematch")
    public ResponseEntity<AnimeRematchResponse> rematch(@PathVariable String slug) {
        return matchingService.rematch(slug)
                .map(AnimeRematchResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Dry-run del matcher sobre todo el catálogo: compara el tmdbId propuesto
     * con el actual sin guardar nada. Operación larga (una búsqueda TMDb por
     * anime); el resumen también queda en logs.
     */
    @GetMapping("/matching/dry-run")
    public ResponseEntity<AnimeMatchingService.DryRunReport> dryRunMatching() {
        return ResponseEntity.ok(matchingService.dryRunMatchAll());
    }
}
