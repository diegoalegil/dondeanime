package com.dondeanime.backend.curated;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/track/lists")
public class CuratedListTrackingController {

    private final CuratedListTrackingService service;

    public CuratedListTrackingController(CuratedListTrackingService service) {
        this.service = service;
    }

    @PostMapping("/view")
    public ResponseEntity<Void> view(@Valid @RequestBody CuratedListMetricRequest request) {
        service.track(request, CuratedListMetricType.VIEW);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/anime-click")
    public ResponseEntity<Void> animeClick(@Valid @RequestBody CuratedListMetricRequest request) {
        service.track(request, CuratedListMetricType.ANIME_CLICK);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/premium-click")
    public ResponseEntity<Void> premiumClick(@Valid @RequestBody CuratedListMetricRequest request) {
        service.track(request, CuratedListMetricType.PREMIUM_CTA_CLICK);
        return ResponseEntity.noContent().build();
    }
}
