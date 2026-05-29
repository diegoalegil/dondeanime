package com.dondeanime.backend.chat;

public record ChatSearchRequest(
        String question,
        String countryCode) {
}
