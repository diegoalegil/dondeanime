package com.dondeanime.backend.push;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mobile/push")
public class MobilePushRegistrationController {

    private final MobilePushRegistrationService registrationService;

    public MobilePushRegistrationController(MobilePushRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<MobilePushRegistrationResponse> register(
            @Valid @RequestBody MobilePushRegistrationRequest request) {
        return ResponseEntity.accepted().body(registrationService.register(request));
    }
}
