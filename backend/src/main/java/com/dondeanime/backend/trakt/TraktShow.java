package com.dondeanime.backend.trakt;

public record TraktShow(
        String title,
        Integer year,
        TraktShowIds ids) {
}
