package com.dondeanime.backend.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TmdbImageUrlsTest {

    @Test
    void buildsAbsoluteUrlFromRelativePath() {
        assertEquals("https://image.tmdb.org/t/p/original/logo.png",
                TmdbImageUrls.fullLogoUrl("/logo.png"));
    }

    @Test
    void returnsNullForNullOrBlankPath() {
        assertNull(TmdbImageUrls.fullLogoUrl(null));
        assertNull(TmdbImageUrls.fullLogoUrl(""));
        assertNull(TmdbImageUrls.fullLogoUrl("   "));
    }
}
