package com.dondeanime.backend.affiliate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dondeanime.backend.anime.RecommendationTrackingService;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AffiliateDashboardController {

    private final AffiliateLinkService affiliateLinkService;
    private final RecommendationTrackingService recommendationTrackingService;

    public AffiliateDashboardController(
            AffiliateLinkService affiliateLinkService,
            RecommendationTrackingService recommendationTrackingService) {
        this.affiliateLinkService = affiliateLinkService;
        this.recommendationTrackingService = recommendationTrackingService;
    }

    @GetMapping
    public AffiliateDashboardDto dashboard() {
        AffiliateDashboardDto affiliateDashboard = affiliateLinkService.dashboard();
        return new AffiliateDashboardDto(
                affiliateDashboard.clicksLast7Days(),
                affiliateDashboard.clicksLast30Days(),
                affiliateDashboard.topAffiliateAnime(),
                affiliateDashboard.topAffiliateLinks(),
                affiliateDashboard.topVisitedAnime(),
                recommendationTrackingService.topRecommendationClicks30Days());
    }
}
