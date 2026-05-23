package com.dondeanime.backend.anime;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/anime")
public class AnimeController {

    private final AnimeRepository repository;

    public AnimeController(AnimeRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Anime> getAll() {
        return repository.findAll();
    }
}
