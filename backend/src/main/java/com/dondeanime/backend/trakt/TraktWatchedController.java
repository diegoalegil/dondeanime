package com.dondeanime.backend.trakt;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TraktWatchedController {

    private final TraktWatchedService traktWatchedService;

    public TraktWatchedController(TraktWatchedService traktWatchedService) {
        this.traktWatchedService = traktWatchedService;
    }

    @GetMapping("/api/trakt/watched")
    public TraktWatchedResponse watched(@RequestParam(required = false) String externalUserId) {
        return traktWatchedService.watched(externalUserId);
    }
}
