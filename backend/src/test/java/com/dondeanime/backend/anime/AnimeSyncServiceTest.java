package com.dondeanime.backend.anime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.dondeanime.backend.anime.anilist.AniListClient;
import com.dondeanime.backend.anime.anilist.AniListCoverImage;
import com.dondeanime.backend.anime.anilist.AniListFuzzyDate;
import com.dondeanime.backend.anime.anilist.AniListMedia;
import com.dondeanime.backend.anime.anilist.AniListStudio;
import com.dondeanime.backend.anime.anilist.AniListStudioConnection;
import com.dondeanime.backend.anime.anilist.AniListTitle;
import com.dondeanime.backend.studio.Studio;
import com.dondeanime.backend.studio.StudioRepository;

class AnimeSyncServiceTest {

    @Test
    void syncPopularAcceptsFiveHundredAnime() {
        AniListClient client = mock(AniListClient.class);
        AnimeRepository repository = mock(AnimeRepository.class);
        StudioRepository studioRepository = mock(StudioRepository.class);
        when(client.fetchPopular(AnimeSyncService.MAX_POPULAR_SYNC_COUNT)).thenReturn(List.of());

        int synced = new AnimeSyncService(client, repository, studioRepository)
                .syncPopular(AnimeSyncService.MAX_POPULAR_SYNC_COUNT);

        assertThat(synced).isZero();
        verify(client).fetchPopular(AnimeSyncService.MAX_POPULAR_SYNC_COUNT);
    }

    @Test
    void syncPopularRejectsCountsAboveSprintLimit() {
        AniListClient client = mock(AniListClient.class);
        AnimeRepository repository = mock(AnimeRepository.class);
        StudioRepository studioRepository = mock(StudioRepository.class);
        AnimeSyncService service = new AnimeSyncService(client, repository, studioRepository);

        assertThatThrownBy(() -> service.syncPopular(501))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("500");

        verify(client, never()).fetchPopular(anyInt());
    }

    @Test
    void syncPopularStoresStudiosFromAnilist() {
        AniListClient client = mock(AniListClient.class);
        AnimeRepository animeRepository = mock(AnimeRepository.class);
        StudioRepository studioRepository = mock(StudioRepository.class);
        AniListMedia media = mediaWithStudio();

        when(client.fetchPopular(1)).thenReturn(List.of(media));
        when(animeRepository.findByAnilistId(16498L)).thenReturn(Optional.empty());
        when(animeRepository.findBySlug("attack-on-titan")).thenReturn(Optional.empty());
        when(studioRepository.findByAnilistId(858L)).thenReturn(Optional.empty());
        when(studioRepository.findBySlug("wit-studio")).thenReturn(Optional.empty());
        when(studioRepository.save(any(Studio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int synced = new AnimeSyncService(client, animeRepository, studioRepository).syncPopular(1);

        assertThat(synced).isEqualTo(1);

        ArgumentCaptor<Studio> studioCaptor = ArgumentCaptor.forClass(Studio.class);
        verify(studioRepository).save(studioCaptor.capture());
        assertThat(studioCaptor.getValue().getName()).isEqualTo("WIT Studio");
        assertThat(studioCaptor.getValue().getSlug()).isEqualTo("wit-studio");
        assertThat(studioCaptor.getValue().isAnimationStudio()).isTrue();

        ArgumentCaptor<Anime> animeCaptor = ArgumentCaptor.forClass(Anime.class);
        verify(animeRepository).save(animeCaptor.capture());
        assertThat(animeCaptor.getValue().getStudios())
                .extracting(Studio::getName)
                .containsExactly("WIT Studio");
    }

    private static AniListMedia mediaWithStudio() {
        return new AniListMedia(
                16498L,
                new AniListTitle("Shingeki no Kyojin", "Attack on Titan"),
                new AniListFuzzyDate(2013, 4, 7),
                null,
                25,
                "TV",
                "FINISHED",
                85,
                500000,
                "Descripción",
                new AniListCoverImage("https://example.com/cover.jpg"),
                null,
                List.of("Action"),
                new AniListStudioConnection(List.of(
                        new AniListStudio(858L, "WIT Studio", true))),
                "SPRING",
                2013);
    }
}
