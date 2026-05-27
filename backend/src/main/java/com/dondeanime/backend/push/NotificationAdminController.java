package com.dondeanime.backend.push;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/notifications")
public class NotificationAdminController {

    private final NotificationDashboardService notificationDashboardService;

    public NotificationAdminController(NotificationDashboardService notificationDashboardService) {
        this.notificationDashboardService = notificationDashboardService;
    }

    @GetMapping("/stats")
    public NotificationStatsDto stats() {
        return notificationDashboardService.stats();
    }

    @PostMapping("/test")
    public NotificationTestResponse testPush(@Valid @RequestBody NotificationTestRequest request) {
        return notificationDashboardService.sendTestPush(request.subscriptionId());
    }
}
