package com.dondeanime.backend.anime;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/anime")
public class AnimeController {

    private final AnimeRepository repository;
    private final AnimeSyncService syncService;

    public AnimeController(AnimeRepository repository, AnimeSyncService syncService) {
        this.repository = repository;
        this.syncService = syncService;
    }

    @GetMapping
    public List<Anime> getAll() {
        return repository.findAll();
    }

    /**
     * Dispara el sync de AniList. Manual durante desarrollo; en semana 4
     * lo automatizaremos con @Scheduled cada 12h.
     *
     * Ejemplo: POST /api/anime/sync?count=100
     */
    @PostMapping("/sync")
    public Map<String, Integer> sync(@RequestParam(defaultValue = "100") int count) {
        int synced = syncService.syncPopular(count);
        return Map.of("synced", synced);
    }
}
