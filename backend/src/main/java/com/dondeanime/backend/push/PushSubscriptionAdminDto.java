package com.dondeanime.backend.push;

import java.time.Instant;

public record PushSubscriptionAdminDto(
        Long id,
        String userEmail,
        String countryIso,
        Instant createdAt,
        Integer deliverySuccessCount,
        Integer deliveryFailureCount,
        Integer lastStatusCode,
        Instant lastDeliveredAt,
        Instant lastFailedAt) {

    public static PushSubscriptionAdminDto from(PushSubscription subscription) {
        return new PushSubscriptionAdminDto(
                subscription.getId(),
                subscription.getUserEmail(),
                subscription.getCountryIso(),
                subscription.getCreatedAt(),
                count(subscription.getDeliverySuccessCount()),
                count(subscription.getDeliveryFailureCount()),
                subscription.getLastStatusCode(),
                subscription.getLastDeliveredAt(),
                subscription.getLastFailedAt());
    }

    private static int count(Integer value) {
        return value == null ? 0 : value;
    }
}
