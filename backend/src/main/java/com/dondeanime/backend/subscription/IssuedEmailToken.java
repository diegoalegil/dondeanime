package com.dondeanime.backend.subscription;

public record IssuedEmailToken(
        String rawToken,
        EmailToken entity
) {}
