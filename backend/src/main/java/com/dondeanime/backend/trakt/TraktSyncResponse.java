package com.dondeanime.backend.trakt;

import java.util.List;

public record TraktSyncResponse(
        int watchedImported,
        int ratingsImported,
        int unmatchedCount,
        List<TraktUnmatchedItem> unmatched) {
}
