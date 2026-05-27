package com.dondeanime.backend.embedding;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;

@Hidden
@RestController
@RequestMapping("/api/admin/embeddings")
public class EmbeddingAdminController {

    private final EmbeddingRebuildService rebuildService;

    public EmbeddingAdminController(EmbeddingRebuildService rebuildService) {
        this.rebuildService = rebuildService;
    }

    @PostMapping("/rebuild")
    public EmbeddingRebuildResponse rebuild() {
        return rebuildService.rebuild();
    }
}
