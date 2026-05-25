package com.dondeanime.backend.anime;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/genres")
public class GenreController {

    private final AnimeRepository animeRepository;

    public GenreController(AnimeRepository animeRepository) {
        this.animeRepository = animeRepository;
    }

    /**
     * Todos los géneros distintos con count de anime.
     * Ordenado por count desc.
     */
    @GetMapping
    public List<GenreSummaryDto> list() {
        return animeRepository.aggregateGenres().stream()
                .map(r -> GenreSummaryDto.of(r.getGenre(), r.getAnimeCount()))
                .toList();
    }

    /**
     * Anime de un género concreto, ordenados por popularidad.
     * Ejemplo: GET /api/genres/action
     */
    @GetMapping("/{slug}")
    public List<AnimeSummaryDto> animesByGenre(@PathVariable String slug) {
        return animeRepository.findByGenreSlug(slug.toLowerCase()).stream()
                .map(AnimeSummaryDto::from)
                .toList();
    }
}
