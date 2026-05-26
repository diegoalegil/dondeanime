package com.dondeanime.backend.newsletter;

public record IssuedNewsletterToken(
        String rawToken,
        NewsletterToken entity
) {}
