package com.dondeanime.backend.push;

public record NotificationTestResponse(
        boolean sent,
        Integer statusCode,
        String message) {
}
