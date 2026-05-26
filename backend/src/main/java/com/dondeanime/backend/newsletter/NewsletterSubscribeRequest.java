package com.dondeanime.backend.newsletter;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NewsletterSubscribeRequest(
        @NotBlank @Email String email,
        @NotNull @AssertTrue Boolean privacyAccepted
) {}
