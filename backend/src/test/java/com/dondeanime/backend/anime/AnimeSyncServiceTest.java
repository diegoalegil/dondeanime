package com.dondeanime.backend.anime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.dondeanime.backend.anime.anilist.AniListClient;

class AnimeSyncServiceTest {

    @Test
    void syncPopularAcceptsFiveHundredAnime() {
        AniListClient client = mock(AniListClient.class);
        AnimeRepository repository = mock(AnimeRepository.class);
        when(client.fetchPopular(AnimeSyncService.MAX_POPULAR_SYNC_COUNT)).thenReturn(List.of());

        int synced = new AnimeSyncService(client, repository)
                .syncPopular(AnimeSyncService.MAX_POPULAR_SYNC_COUNT);

        assertThat(synced).isZero();
        verify(client).fetchPopular(AnimeSyncService.MAX_POPULAR_SYNC_COUNT);
    }

    @Test
    void syncPopularRejectsCountsAboveSprintLimit() {
        AniListClient client = mock(AniListClient.class);
        AnimeRepository repository = mock(AnimeRepository.class);
        AnimeSyncService service = new AnimeSyncService(client, repository);

        assertThatThrownBy(() -> service.syncPopular(501))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("500");

        verify(client, never()).fetchPopular(anyInt());
    }
}
