package com.dondeanime.backend.affiliate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AffiliateDashboardController {

    private final AffiliateLinkService affiliateLinkService;

    public AffiliateDashboardController(AffiliateLinkService affiliateLinkService) {
        this.affiliateLinkService = affiliateLinkService;
    }

    @GetMapping
    public AffiliateDashboardDto dashboard() {
        return affiliateLinkService.dashboard();
    }
}
