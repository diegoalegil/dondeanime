package com.dondeanime.backend.push;

public record PushNotificationPayload(
        String title,
        String body,
        String url,
        String tag) {
}
