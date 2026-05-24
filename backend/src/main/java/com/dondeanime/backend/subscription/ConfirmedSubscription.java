package com.dondeanime.backend.subscription;

public record ConfirmedSubscription(
        String email,
        String animeTitle,
        String countryName
) {}
