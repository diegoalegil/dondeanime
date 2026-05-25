package com.dondeanime.backend.sitemap;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dondeanime.backend.anime.AnimeRepository;
import com.dondeanime.backend.anime.GenreSummaryDto;
import com.dondeanime.backend.provider.ProviderSummaryDto;
import com.dondeanime.backend.provider.WatchProviderRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Sitemap", description = "Datos para generar sitemap estatico del frontend")
public class SitemapController {

    private final AnimeRepository animeRepository;
    private final WatchProviderRepository providerRepository;

    public SitemapController(AnimeRepository animeRepository, WatchProviderRepository providerRepository) {
        this.animeRepository = animeRepository;
        this.providerRepository = providerRepository;
    }

    /**
     * Datos para generar sitemap.xml desde el frontend.
     * Una sola request te da todo: slugs de anime, pares
     * (proveedor, país), slugs de géneros y temporadas.
     */
    @GetMapping("/api/v1/sitemap")
    @Operation(summary = "Lista identificadores para sitemap", description = "Devuelve slugs y pares necesarios para generar sitemap.xml.")
    public SitemapDto sitemap() {
        List<String> animeSlugs = animeRepository.findAllSlugs();

        List<SitemapDto.ProviderCountryEntry> providers = providerRepository
                .aggregateProviderCountries().stream()
                .map(r -> new SitemapDto.ProviderCountryEntry(
                        ProviderSummaryDto.slugify(r.getProviderName()),
                        r.getCountryCode()))
                .toList();

        List<String> genreSlugs = animeRepository.aggregateGenres().stream()
                .map(r -> GenreSummaryDto.slugify(r.getGenre()))
                .toList();

        List<SitemapDto.SeasonEntry> seasons = animeRepository.aggregateSeasons().stream()
                .map(r -> new SitemapDto.SeasonEntry(r.getYear(), r.getSeason()))
                .toList();

        return new SitemapDto(animeSlugs, providers, genreSlugs, seasons);
    }
}
