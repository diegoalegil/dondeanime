package com.dondeanime.backend.curated;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping({"/api/lists", "/api/v1/lists"})
@Tag(name = "Curated lists", description = "Listas publicas de anime curadas")
public class CuratedListController {

    private final CuratedListService service;

    public CuratedListController(CuratedListService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista listas curadas publicadas", description = "Devuelve solo listas publicas y publicadas.")
    public List<CuratedListSummaryDto> list() {
        return service.publishedLists();
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Detalle de lista curada", description = "Devuelve anime ordenados y schema.org ItemList.")
    public ResponseEntity<CuratedListDetailDto> detail(@PathVariable String slug) {
        return service.publishedList(slug)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
