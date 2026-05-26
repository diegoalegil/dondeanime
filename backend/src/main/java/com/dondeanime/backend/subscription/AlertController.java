package com.dondeanime.backend.subscription;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final SubscriptionService subscriptionService;

    public AlertController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> create(@Valid @RequestBody SubscriptionRequest request) {
        subscriptionService.requestSubscription(request);
        return ResponseEntity.accepted().body(new SubscriptionResponse(
                "Si el email es valido, recibiras un correo de confirmacion."));
    }
}
