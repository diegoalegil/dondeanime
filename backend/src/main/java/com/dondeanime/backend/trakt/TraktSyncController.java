package com.dondeanime.backend.trakt;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TraktSyncController {

    private final TraktSyncService traktSyncService;

    public TraktSyncController(TraktSyncService traktSyncService) {
        this.traktSyncService = traktSyncService;
    }

    @PostMapping("/api/trakt/sync")
    public TraktSyncResponse sync(@RequestBody TraktSyncRequest request) {
        return traktSyncService.sync(request);
    }
}
