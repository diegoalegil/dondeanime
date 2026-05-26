package com.dondeanime.backend.anime;

import java.util.Comparator;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/studios")
public class StudioController {

    private final AnimeRepository animeRepository;

    public StudioController(AnimeRepository animeRepository) {
        this.animeRepository = animeRepository;
    }

    @GetMapping
    public List<StudioSummaryDto> list() {
        return animeRepository.aggregateStudios().stream()
                .map(row -> StudioSummaryDto.of(row.getStudio(), row.getAnimeCount()))
                .toList();
    }

    @GetMapping("/{slug}/best")
    public List<AnimeSummaryDto> bestByStudio(@PathVariable String slug) {
        String normalizedSlug = slug.toLowerCase();
        return animeRepository.findAll().stream()
                .filter(anime -> anime.getStudio() != null && !anime.getStudio().isBlank())
                .filter(anime -> normalizedSlug.equals(StudioSummaryDto.slugify(anime.getStudio())))
                .sorted(Comparator
                        .comparing((Anime anime) -> anime.getPopularity() == null ? 0 : anime.getPopularity())
                        .reversed()
                        .thenComparing(Anime::getTitleEnglish, Comparator.nullsLast(String::compareTo)))
                .map(AnimeSummaryDto::from)
                .toList();
    }
}
