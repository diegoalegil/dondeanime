package com.dondeanime.backend.push;

import jakarta.validation.constraints.NotBlank;

public record PushSubscriptionKeys(
        @NotBlank String p256dh,
        @NotBlank String auth) {
}
