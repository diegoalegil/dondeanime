package com.dondeanime.backend.curated;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/admin/lists")
public class CuratedListAdminController {

    private final CuratedListService service;

    public CuratedListAdminController(CuratedListService service) {
        this.service = service;
    }

    @GetMapping
    public List<CuratedListAdminDto> list() {
        return service.adminLists();
    }

    @PostMapping
    public CuratedListAdminDto save(@Valid @RequestBody CuratedListSaveRequest request) {
        return service.saveAdminList(request);
    }

    @PostMapping("/{slug}/items")
    public ResponseEntity<CuratedListAdminDto> addItem(
            @PathVariable String slug,
            @Valid @RequestBody CuratedListItemSaveRequest request) {
        return service.addAdminItem(slug, request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{slug}/items/{animeSlug}/up")
    public ResponseEntity<CuratedListAdminDto> moveItemUp(
            @PathVariable String slug,
            @PathVariable String animeSlug) {
        return service.moveAdminItem(slug, animeSlug, -1)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{slug}/items/{animeSlug}/down")
    public ResponseEntity<CuratedListAdminDto> moveItemDown(
            @PathVariable String slug,
            @PathVariable String animeSlug) {
        return service.moveAdminItem(slug, animeSlug, 1)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{slug}/items/{animeSlug}")
    public ResponseEntity<CuratedListAdminDto> deleteItem(
            @PathVariable String slug,
            @PathVariable String animeSlug) {
        return service.deleteAdminItem(slug, animeSlug)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{slug}/publish")
    public ResponseEntity<CuratedListAdminDto> publish(@PathVariable String slug) {
        return service.publishAdminList(slug)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }
}
