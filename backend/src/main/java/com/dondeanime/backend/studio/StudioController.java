package com.dondeanime.backend.studio;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dondeanime.backend.anime.AnimeRepository;
import com.dondeanime.backend.anime.AnimeSummaryDto;

@RestController
@RequestMapping("/api/studios")
public class StudioController {

    private final StudioRepository studioRepository;
    private final AnimeRepository animeRepository;

    public StudioController(StudioRepository studioRepository, AnimeRepository animeRepository) {
        this.studioRepository = studioRepository;
        this.animeRepository = animeRepository;
    }

    @GetMapping
    public List<StudioSummaryDto> list() {
        return studioRepository.aggregateStudios().stream()
                .map(row -> StudioSummaryDto.from(row.getStudio(), row.getAnimeCount()))
                .toList();
    }

    @GetMapping("/{slug}")
    public List<AnimeSummaryDto> animesByStudio(@PathVariable String slug) {
        return animeRepository.findByStudioSlug(slug.toLowerCase()).stream()
                .map(AnimeSummaryDto::from)
                .toList();
    }

    @GetMapping("/{slug}/best")
    public List<AnimeSummaryDto> bestByStudio(@PathVariable String slug) {
        return animeRepository.findByStudioSlug(slug.toLowerCase()).stream()
                .map(AnimeSummaryDto::from)
                .toList();
    }
}
