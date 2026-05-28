package com.dondeanime.backend.trakt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

class TraktWatchedServiceTest {

    private final UserWatchedAnimeRepository watchedAnimeRepository = mock(UserWatchedAnimeRepository.class);
    private final TraktWatchedService service = new TraktWatchedService(watchedAnimeRepository);

    @Test
    void returnsWatchedSlugsForExternalUser() {
        when(watchedAnimeRepository.findWatchedSlugs("trakt", "user-123"))
                .thenReturn(List.of("attack-on-titan", "death-note"));

        TraktWatchedResponse response = service.watched(" user-123 ");

        assertThat(response.slugs()).containsExactly("attack-on-titan", "death-note");
    }

    @Test
    void blankExternalUserReturnsEmptyList() {
        TraktWatchedResponse response = service.watched(" ");

        assertThat(response.slugs()).isEmpty();
        verifyNoInteractions(watchedAnimeRepository);
    }
}
