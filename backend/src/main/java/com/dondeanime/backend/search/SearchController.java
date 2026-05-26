package com.dondeanime.backend.search;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dondeanime.backend.anime.AnimeSummaryDto;

@RestController
public class SearchController {

    private final AnimeSearchService searchService;

    public SearchController(AnimeSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/api/search")
    public List<AnimeSummaryDto> search(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(defaultValue = "10") Integer limit) {
        return searchService.search(query, limit);
    }
}
