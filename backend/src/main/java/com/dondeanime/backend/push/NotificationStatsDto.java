package com.dondeanime.backend.push;

import java.util.List;

public record NotificationStatsDto(
        long activeSubscriptions,
        long alertsSentLast24Hours,
        long deliveryAttempts,
        long deliverySuccesses,
        long deliveryFailures,
        double deliveryRatePercent,
        boolean pushConfigured,
        List<PushSubscriptionAdminDto> subscriptions) {
}
