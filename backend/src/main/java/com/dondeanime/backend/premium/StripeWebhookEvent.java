package com.dondeanime.backend.premium;

import java.time.Instant;

public record StripeWebhookEvent(
        String type,
        String email,
        String customerId,
        Instant eventTime,
        Instant currentPeriodEnd,
        String eventId) {
}
