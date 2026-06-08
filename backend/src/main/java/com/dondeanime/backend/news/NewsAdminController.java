package com.dondeanime.backend.news;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;

/**
 * Endpoints admin del News Engine. Protegido por la cadena de seguridad
 * ({@code /api/admin/**} exige JWT). {@code @Hidden} lo oculta del OpenAPI público.
 */
@Hidden
@RestController
@RequestMapping("/api/admin/news")
public class NewsAdminController {

    private final NewsIngestionService ingestionService;
    private final NewsProcessingService processingService;

    public NewsAdminController(NewsIngestionService ingestionService, NewsProcessingService processingService) {
        this.ingestionService = ingestionService;
        this.processingService = processingService;
    }

    /** Dispara una pasada de ingesta RSS manualmente. */
    @PostMapping("/ingest")
    public NewsIngestionResult ingest() {
        return ingestionService.ingestAll();
    }

    /** Enriquece borradores RSS y, solo con flag explicita, los publica. */
    @PostMapping("/process")
    public NewsProcessingResult process() {
        return processingService.processDrafts();
    }
}
