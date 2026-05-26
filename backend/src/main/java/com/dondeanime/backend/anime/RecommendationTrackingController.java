package com.dondeanime.backend.anime;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/track")
public class RecommendationTrackingController {

    private final RecommendationTrackingService trackingService;

    public RecommendationTrackingController(RecommendationTrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @PostMapping("/recommendation")
    public ResponseEntity<Void> trackRecommendation(@Valid @RequestBody RecommendationTrackRequest request) {
        trackingService.trackClick(request);
        return ResponseEntity.noContent().build();
    }
}
