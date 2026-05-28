package com.dondeanime.backend.push;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MobilePushRegistrationRequest(
        @NotBlank @Pattern(regexp = "^(ios|android|IOS|ANDROID)$") String platform,
        @NotBlank @Size(max = 512) String deviceToken,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$") String countryIso) {
}
