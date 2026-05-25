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

import com.dondeanime.backend.anime.anilist.AniListCharacter;
import com.dondeanime.backend.anime.anilist.AniListCharacterConnection;
import com.dondeanime.backend.anime.anilist.AniListCharacterEdge;
import com.dondeanime.backend.anime.anilist.AniListCharacterImage;
import com.dondeanime.backend.anime.anilist.AniListCharacterName;
import com.dondeanime.backend.anime.anilist.AniListClient;
import com.dondeanime.backend.anime.anilist.AniListCoverImage;
import com.dondeanime.backend.anime.anilist.AniListFuzzyDate;
import com.dondeanime.backend.anime.anilist.AniListMedia;
import com.dondeanime.backend.anime.anilist.AniListStudio;
import com.dondeanime.backend.anime.anilist.AniListStudioConnection;
import com.dondeanime.backend.anime.anilist.AniListTitle;
import com.dondeanime.backend.character.AnimeCharacter;
import com.dondeanime.backend.character.AnimeCharacterRepository;
import com.dondeanime.backend.character.AnimeCharacterRole;
import com.dondeanime.backend.studio.Studio;
import com.dondeanime.backend.studio.StudioRepository;

class AnimeSyncServiceTest {

    @Test
    void syncPopularAcceptsFiveHundredAnime() {
        AniListClient client = mock(AniListClient.class);
        AnimeRepository repository = mock(AnimeRepository.class);
        StudioRepository studioRepository = mock(StudioRepository.class);
        AnimeCharacterRepository characterRepository = mock(AnimeCharacterRepository.class);
        when(client.fetchPopular(AnimeSyncService.MAX_POPULAR_SYNC_COUNT)).thenReturn(List.of());

        int synced = new AnimeSyncService(client, repository, studioRepository, characterRepository)
                .syncPopular(AnimeSyncService.MAX_POPULAR_SYNC_COUNT);

        assertThat(synced).isZero();
        verify(client).fetchPopular(AnimeSyncService.MAX_POPULAR_SYNC_COUNT);
    }

    @Test
    void syncPopularRejectsCountsAboveSprintLimit() {
        AniListClient client = mock(AniListClient.class);
        AnimeRepository repository = mock(AnimeRepository.class);
        StudioRepository studioRepository = mock(StudioRepository.class);
        AnimeCharacterRepository characterRepository = mock(AnimeCharacterRepository.class);
        AnimeSyncService service = new AnimeSyncService(client, repository, studioRepository, characterRepository);

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
        AnimeCharacterRepository characterRepository = mock(AnimeCharacterRepository.class);
        AniListMedia media = mediaWithStudio();

        when(client.fetchPopular(1)).thenReturn(List.of(media));
        when(animeRepository.findByAnilistIdWithCharacters(16498L)).thenReturn(Optional.empty());
        when(animeRepository.findBySlug("attack-on-titan")).thenReturn(Optional.empty());
        when(studioRepository.findByAnilistId(858L)).thenReturn(Optional.empty());
        when(studioRepository.findBySlug("wit-studio")).thenReturn(Optional.empty());
        when(studioRepository.save(any(Studio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int synced = new AnimeSyncService(client, animeRepository, studioRepository, characterRepository).syncPopular(1);

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

    @Test
    void syncPopularStoresMainCharactersFromAnilist() {
        AniListMedia media = mediaWithCharacters(List.of(
                edge(40882L, "Eren Yeager", "https://img.example/eren.jpg"),
                edge(40881L, "Mikasa Ackerman", "https://img.example/mikasa.jpg")
        ));

        AniListClient client = mock(AniListClient.class);
        when(client.fetchPopular(1)).thenReturn(List.of(media));
        AnimeRepository repository = mock(AnimeRepository.class);
        when(repository.findByAnilistIdWithCharacters(16498L)).thenReturn(Optional.empty());
        when(repository.findBySlug("attack-on-titan")).thenReturn(Optional.empty());
        StudioRepository studioRepository = mock(StudioRepository.class);
        AnimeCharacterRepository characterRepository = mock(AnimeCharacterRepository.class);
        when(characterRepository.findByAnilistId(any())).thenReturn(Optional.empty());
        when(characterRepository.save(any(AnimeCharacter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int synced = new AnimeSyncService(client, repository, studioRepository, characterRepository).syncPopular(1);

        ArgumentCaptor<Anime> captor = ArgumentCaptor.forClass(Anime.class);
        verify(repository).save(captor.capture());
        Anime anime = captor.getValue();
        assertThat(synced).isEqualTo(1);
        assertThat(anime.getCharacterRoles())
                .extracting(role -> role.getCharacter().getName())
                .containsExactlyInAnyOrder("Eren Yeager", "Mikasa Ackerman");
        assertThat(anime.getCharacterRoles())
                .extracting(AnimeCharacterRole::getRole)
                .containsOnly("MAIN");
    }

    @Test
    void syncPopularLimitsCharactersToSix() {
        AniListMedia media = mediaWithCharacters(List.of(
                edge(1L, "Uno", null),
                edge(2L, "Dos", null),
                edge(3L, "Tres", null),
                edge(4L, "Cuatro", null),
                edge(5L, "Cinco", null),
                edge(6L, "Seis", null),
                edge(7L, "Siete", null)
        ));

        AniListClient client = mock(AniListClient.class);
        when(client.fetchPopular(1)).thenReturn(List.of(media));
        AnimeRepository repository = mock(AnimeRepository.class);
        when(repository.findByAnilistIdWithCharacters(16498L)).thenReturn(Optional.empty());
        when(repository.findBySlug("attack-on-titan")).thenReturn(Optional.empty());
        StudioRepository studioRepository = mock(StudioRepository.class);
        AnimeCharacterRepository characterRepository = mock(AnimeCharacterRepository.class);
        when(characterRepository.findByAnilistId(any())).thenReturn(Optional.empty());
        when(characterRepository.save(any(AnimeCharacter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        new AnimeSyncService(client, repository, studioRepository, characterRepository).syncPopular(1);

        ArgumentCaptor<Anime> captor = ArgumentCaptor.forClass(Anime.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCharacterRoles()).hasSize(6);
    }

    private static AniListMedia mediaWithStudio() {
        return media(List.of(
                new AniListStudio(858L, "WIT Studio", true)
        ), List.of());
    }

    private static AniListMedia mediaWithCharacters(List<AniListCharacterEdge> characterEdges) {
        return media(List.of(), characterEdges);
    }

    private static AniListMedia media(List<AniListStudio> studios, List<AniListCharacterEdge> characterEdges) {
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
                "Descripcion",
                new AniListCoverImage("https://example.com/cover.jpg"),
                null,
                List.of("Action"),
                new AniListStudioConnection(studios),
                "SPRING",
                2013,
                new AniListCharacterConnection(characterEdges)
        );
    }

    private static AniListCharacterEdge edge(Long id, String name, String image) {
        return new AniListCharacterEdge(
                "MAIN",
                new AniListCharacter(
                        id,
                        new AniListCharacterName(name, null),
                        new AniListCharacterImage(image, null)));
    }
}
