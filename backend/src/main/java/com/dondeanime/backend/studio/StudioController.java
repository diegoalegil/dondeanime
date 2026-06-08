package com.dondeanime.backend.studio;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dondeanime.backend.anime.AnimeRepository;
import com.dondeanime.backend.anime.AnimeSummaryDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping({"/api/studios", "/api/v1/studios"})
@Tag(name = "Studios", description = "Estudios de anime disponibles en el catalogo")
public class StudioController {

    private final StudioRepository studioRepository;
    private final AnimeRepository animeRepository;

    public StudioController(StudioRepository studioRepository, AnimeRepository animeRepository) {
        this.studioRepository = studioRepository;
        this.animeRepository = animeRepository;
    }

    @GetMapping
    @Operation(summary = "Lista estudios", description = "Devuelve estudios agregados con numero de anime.")
    public List<StudioSummaryDto> list() {
        return studioRepository.aggregateStudios().stream()
                .map(row -> StudioSummaryDto.from(row.getStudio(), row.getAnimeCount()))
                .toList();
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Lista anime por estudio", description = "Devuelve anime asociados a un estudio.")
    public List<AnimeSummaryDto> animesByStudio(@PathVariable String slug) {
        return animeRepository.findByStudioSlug(slug.toLowerCase()).stream()
                .map(AnimeSummaryDto::from)
                .toList();
    }

    @GetMapping("/{slug}/best")
    @Operation(summary = "Lista mejores anime por estudio", description = "Devuelve anime destacados asociados a un estudio.")
    public List<AnimeSummaryDto> bestByStudio(@PathVariable String slug) {
        return animeRepository.findByStudioSlug(slug.toLowerCase()).stream()
                .map(AnimeSummaryDto::from)
                .toList();
    }
}
