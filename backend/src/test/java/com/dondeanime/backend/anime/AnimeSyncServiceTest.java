package com.dondeanime.backend.anime;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.dondeanime.backend.anime.anilist.AniListTitle;

class AnimeSyncServiceTest {

    private final AniListClient client = mock(AniListClient.class);
    private final AnimeRepository repository = mock(AnimeRepository.class);
    private final AnimeSyncService service = new AnimeSyncService(client, repository);

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
                2006);
    }
}
