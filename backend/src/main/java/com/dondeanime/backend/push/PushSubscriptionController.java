package com.dondeanime.backend.push;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/push")
public class PushSubscriptionController {

    private final WebPushService webPushService;

    public PushSubscriptionController(WebPushService webPushService) {
        this.webPushService = webPushService;
    }

    @PostMapping("/subscribe")
    public ResponseEntity<PushSubscriptionResponse> subscribe(@Valid @RequestBody PushSubscriptionRequest request) {
        webPushService.saveSubscription(request);
        return ResponseEntity.accepted().body(new PushSubscriptionResponse(
                "Suscripcion push guardada."));
    }
}
