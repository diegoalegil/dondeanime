package com.dondeanime.backend.premium;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/premium")
public class PremiumController {

    private final StripeService stripeService;

    public PremiumController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @PostMapping("/checkout")
    public PremiumCheckoutResponse checkout(@Valid @RequestBody PremiumCheckoutRequest request) {
        return new PremiumCheckoutResponse(stripeService.createCheckoutSession(
                request.email(),
                request.sourceListSlug()));
    }

    @PostMapping("/portal")
    public PremiumPortalResponse portal(@Valid @RequestBody PremiumPortalRequest request) {
        return new PremiumPortalResponse(stripeService.createCustomerPortalSession(request.email()));
    }

    @PostMapping("/webhook")
    public ResponseEntity<PremiumWebhookResponse> webhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        return ResponseEntity.ok(new PremiumWebhookResponse(stripeService.handleWebhook(payload, signature)));
    }
}
