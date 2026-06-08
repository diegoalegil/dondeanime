package com.dondeanime.backend.premium;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final PremiumAccessService premiumAccessService;

    public PremiumController(StripeService stripeService, PremiumAccessService premiumAccessService) {
        this.stripeService = stripeService;
        this.premiumAccessService = premiumAccessService;
    }

    @PostMapping("/checkout")
    public PremiumCheckoutResponse checkout(@Valid @RequestBody PremiumCheckoutRequest request) {
        return new PremiumCheckoutResponse(stripeService.createCheckoutSession(
                request.email(),
                request.sourceListSlug()));
    }

    @PostMapping("/portal")
    public PremiumPortalResponse portal(@Valid @RequestBody PremiumPortalRequest request) {
        // Si hay suscripción activa, enviamos el enlace al email del titular.
        // Respuesta siempre genérica para no revelar quién es suscriptor.
        stripeService.requestCustomerPortalLink(request.email());
        return new PremiumPortalResponse("email_sent");
    }

    @PostMapping("/access-link")
    public PremiumAccessLinkResponse accessLink(@Valid @RequestBody PremiumPortalRequest request) {
        premiumAccessService.requestAccessLink(request.email());
        return new PremiumAccessLinkResponse("email_sent");
    }

    @GetMapping("/status")
    public PremiumStatusResponse status(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return premiumAccessService.status(authorization);
    }

    @PostMapping("/webhook")
    public ResponseEntity<PremiumWebhookResponse> webhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        return ResponseEntity.ok(new PremiumWebhookResponse(stripeService.handleWebhook(payload, signature)));
    }
}
