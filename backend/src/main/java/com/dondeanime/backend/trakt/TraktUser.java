package com.dondeanime.backend.trakt;

public record TraktUser(
        String username,
        TraktUserIds ids) {
}
