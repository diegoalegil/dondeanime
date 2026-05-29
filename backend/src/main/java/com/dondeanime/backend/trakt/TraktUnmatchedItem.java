package com.dondeanime.backend.trakt;

public record TraktUnmatchedItem(
        String type,
        String title,
        Integer year,
        String reason) {
}
