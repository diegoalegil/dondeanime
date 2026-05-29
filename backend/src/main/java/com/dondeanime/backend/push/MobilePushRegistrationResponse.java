package com.dondeanime.backend.push;

public record MobilePushRegistrationResponse(
        String platform,
        String countryIso,
        Boolean alertsOnly,
        String message) {
}
