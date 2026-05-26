package com.dondeanime.backend.anime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import com.dondeanime.backend.anime.anilist.AniListTag;
import com.dondeanime.backend.anime.anilist.AniListTitle;

class AnimeSyncServiceTest {

    private final AniListClient client = mock(AniListClient.class);
    private final AnimeRepository repository = mock(AnimeRepository.class);
    private final AnimeSyncService service = new AnimeSyncService(client, repository);

    @Test
    void syncPopularPersistsAniListTags() {
        AniListMedia media = new AniListMedia(
                101L,
                new AniListTitle("Steins;Gate", "Steins;Gate"),
                new AniListFuzzyDate(2011, 4, 6),
                null,
                24,
                null,
                null,
                "TV",
                "FINISHED",
                91,
                1000,
                "Time travel thriller",
                new AniListCoverImage("https://img.example/cover.jpg"),
                null,
                List.of("Thriller"),
                "SPRING",
                2011,
                List.of(new AniListTag("Time Travel", 94), new AniListTag("Alternate Universe", 71)));

        when(client.fetchPopular(1)).thenReturn(List.of(media));
        when(repository.findByAnilistId(101L)).thenReturn(Optional.empty());
        when(repository.findBySlug("steinsgate")).thenReturn(Optional.empty());
        when(repository.save(any(Anime.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int saved = service.syncPopular(1);

        ArgumentCaptor<Anime> animeCaptor = ArgumentCaptor.forClass(Anime.class);
        verify(repository).save(animeCaptor.capture());

        assertThat(saved).isEqualTo(1);
        assertThat(animeCaptor.getValue().getTags())
                .containsExactlyInAnyOrder(
                        new AnimeTag("Time Travel", 94),
                        new AnimeTag("Alternate Universe", 71));
    }

    @Test
    void syncPopularStoresMainStudioFromAniList() {
        AniListMedia media = mediaWithStudio("Madhouse");
        when(client.fetchPopular(1)).thenReturn(List.of(media));
        when(repository.findByAnilistId(1L)).thenReturn(Optional.empty());
        when(repository.findBySlug("death-note")).thenReturn(Optional.empty());

        int synced = service.syncPopular(1);

        ArgumentCaptor<Anime> captor = ArgumentCaptor.forClass(Anime.class);
        verify(repository).save(captor.capture());
        assertThat(synced).isEqualTo(1);
        assertThat(captor.getValue().getStudio()).isEqualTo("Madhouse");
    }

    private static AniListMedia mediaWithStudio(String studio) {
        return new AniListMedia(
                1L,
                new AniListTitle("Death Note", "Death Note"),
                new AniListFuzzyDate(2006, 10, 4),
                null,
                37,
                24,
                new AniListStudioConnection(List.of(new AniListStudio(studio))),
                "TV",
                "FINISHED",
                84,
                100000,
                "Descripcion",
                new AniListCoverImage("https://example.com/cover.jpg"),
                null,
                List.of("Mystery"),
                "FALL",
                2006,
                List.of());
    }
}
