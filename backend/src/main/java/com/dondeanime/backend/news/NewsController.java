package com.dondeanime.backend.news;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/news", "/api/v1/news"})
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public List<NewsSummaryDto> list(@RequestParam(defaultValue = "30") int limit) {
        return newsService.latestPublished(limit);
    }

    @GetMapping("/anime/{slug}")
    public List<NewsSummaryDto> byAnimeSlug(@PathVariable String slug) {
        return newsService.publishedForAnimeSlug(slug);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<NewsDetailDto> bySlug(@PathVariable String slug) {
        return newsService.publishedBySlug(slug)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
