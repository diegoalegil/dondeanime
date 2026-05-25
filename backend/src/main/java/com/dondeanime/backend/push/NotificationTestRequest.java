package com.dondeanime.backend.push;

import jakarta.validation.constraints.NotNull;

public record NotificationTestRequest(
        @NotNull Long subscriptionId) {
}
