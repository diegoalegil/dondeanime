package com.dondeanime.backend.provider;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dondeanime.backend.anime.AnimeRepository;
import com.dondeanime.backend.anime.AnimeSummaryDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping({"/api/providers", "/api/v1/providers"})
@Tag(name = "Providers", description = "Plataformas de streaming por pais")
public class ProviderController {

    private final WatchProviderRepository providerRepository;
    private final AnimeRepository animeRepository;

    public ProviderController(WatchProviderRepository providerRepository, AnimeRepository animeRepository) {
        this.providerRepository = providerRepository;
        this.animeRepository = animeRepository;
    }

    /**
     * Lista de plataformas de streaming distintas.
     * ?country=ES filtra solo las disponibles en ese país.
     * Sin parámetro, agrega a nivel global (todos los países).
     */
    @GetMapping
    @Operation(summary = "Lista plataformas", description = "Agrega plataformas globales o filtradas por pais.")
    public List<ProviderSummaryDto> list(@RequestParam(required = false) String country) {
        List<WatchProviderRepository.ProviderAggregation> rows = country == null
                ? providerRepository.aggregateAllProviders()
                : providerRepository.aggregateProvidersByCountry(country.toUpperCase());

        return rows.stream()
                .map(r -> ProviderSummaryDto.of(r.getProviderName(), r.getLogoUrl(), r.getAnimeCount()))
                .toList();
    }

    /**
     * Anime disponibles en una plataforma en un país concreto.
     * Ejemplo: GET /api/providers/crunchyroll/ES
     */
    @GetMapping("/{slug}/{country}")
    @Operation(summary = "Lista anime por plataforma y pais", description = "Devuelve anime disponibles en una plataforma concreta para un pais.")
    public List<AnimeSummaryDto> animesByProviderAndCountry(
            @PathVariable String slug,
            @PathVariable String country) {
        return animeRepository
                .findByProviderSlugAndCountry(slug.toLowerCase(), country.toUpperCase())
                .stream()
                .map(AnimeSummaryDto::from)
                .toList();
    }
}
