package com.dondeanime.backend.affiliate;

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
public class AffiliateTrackingController {

    private final AffiliateLinkService affiliateLinkService;

    public AffiliateTrackingController(AffiliateLinkService affiliateLinkService) {
        this.affiliateLinkService = affiliateLinkService;
    }

    @PostMapping("/affiliate")
    public ResponseEntity<Void> trackAffiliate(@Valid @RequestBody AffiliateTrackRequest request) {
        affiliateLinkService.trackClick(request);
        return ResponseEntity.noContent().build();
    }
}
