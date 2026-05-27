package com.dondeanime.backend.trakt;

import java.util.List;

public record TraktWatchedResponse(
        List<String> slugs) {
}
