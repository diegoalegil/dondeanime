package com.dondeanime.backend.premium;

public record StripeCheckoutCommand(
        String apiKey,
        String priceId,
        String email,
        String successUrl,
        String cancelUrl) {
}
