package com.dondeanime.backend.premium;

import java.time.Instant;

public record StripeWebhookEvent(
        String type,
        String email,
        String customerId,
        String sourceListSlug,
        Instant eventTime,
        Instant currentPeriodEnd,
        String eventId) {
}
