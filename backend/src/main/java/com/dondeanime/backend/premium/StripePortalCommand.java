package com.dondeanime.backend.premium;

public record StripePortalCommand(
        String apiKey,
        String customerId,
        String returnUrl) {
}
