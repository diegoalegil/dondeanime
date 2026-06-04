package com.dondeanime.backend.anime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.dondeanime.backend.AbstractIntegrationTest;
import com.dondeanime.backend.character.AnimeCharacterRole;
import com.dondeanime.backend.studio.Studio;

import io.github.diegoalegil.tsunagi.anilist.AniListCharacter;
import io.github.diegoalegil.tsunagi.anilist.AniListCharacterConnection;
import io.github.diegoalegil.tsunagi.anilist.AniListCharacterEdge;
import io.github.diegoalegil.tsunagi.anilist.AniListCharacterImage;
import io.github.diegoalegil.tsunagi.anilist.AniListCharacterName;
import io.github.diegoalegil.tsunagi.anilist.AniListClient;
import io.github.diegoalegil.tsunagi.anilist.AniListCoverImage;
import io.github.diegoalegil.tsunagi.anilist.AniListFuzzyDate;
import io.github.diegoalegil.tsunagi.anilist.AniListMedia;
import io.github.diegoalegil.tsunagi.anilist.AniListStudio;
import io.github.diegoalegil.tsunagi.anilist.AniListStudioConnection;
import io.github.diegoalegil.tsunagi.anilist.AniListTag;
import io.github.diegoalegil.tsunagi.anilist.AniListTitle;

/**
 * Reproduce contra Postgres real el bug de re-sync: volver a sincronizar un
 * anime que ya existe reasignaba sus studios/characterRoles y chocaba con los
 * unique constraints de las tablas join (uk_anime_studio,
 * uk_anime_character_role), de modo que el segundo sync devolvía {@code 0}.
 *
 * <p>El fix carga las colecciones en el loader y, en updates, las vacía con
 * {@code saveAndFlush} (DELETE) antes de reinsertarlas (INSERT). Este test
 * necesita una BD real porque el fallo está en el orden de operaciones del
 * flush de Hibernate, que un mock no ejercita.
 */
@SpringBootTest
class AnimeSyncServiceResyncIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AnimeSyncService service;

    @Autowired
    private AnimeRepository animeRepository;

    @MockitoBean
    private AniListClient aniListClient;

    @BeforeEach
    void clean() {
        animeRepository.deleteAll();
    }

    @Test
    void resyncingExistingAnimeDoesNotViolateJoinTableConstraints() {
        when(aniListClient.fetchPopular(1)).thenReturn(List.of(media()));

        int first = service.syncPopular(1);
        int second = service.syncPopular(1); // re-sync del mismo: antes devolvía 0 por uk_* duplicate

        assertThat(first).isEqualTo(1);
        assertThat(second).isEqualTo(1);

        Anime anime = animeRepository.findBySlugWithCharacters("attack-on-titan").orElseThrow();
        assertThat(anime.getStudios())
                .extracting(Studio::getName)
                .containsExactly("WIT Studio");
        assertThat(anime.getCharacterRoles())
                .extracting(role -> role.getCharacter().getName())
                .containsExactlyInAnyOrder("Eren Yeager", "Mikasa Ackerman");
        assertThat(anime.getCharacterRoles())
                .extracting(AnimeCharacterRole::getRole)
                .containsOnly("MAIN");
        assertThat(anime.getTitleNative()).isEqualTo("進撃の巨人");
        assertThat(animeRepository.findBySlugWithSynonyms("attack-on-titan").orElseThrow().getSynonyms())
                .contains("AoT", "Ataque a los Titanes");
    }

    private static AniListMedia media() {
        return new AniListMedia(
                16498L,
                new AniListTitle("Shingeki no Kyojin", "Attack on Titan", "進撃の巨人"),
                new AniListFuzzyDate(2013, 4, 7),
                null,
                25,
                24,
                "TV",
                "FINISHED",
                85,
                500000,
                "Descripcion",
                new AniListCoverImage("https://example.com/cover.jpg"),
                null,
                List.of("Action"),
                List.of("AoT", "Ataque a los Titanes"),
                new AniListStudioConnection(List.of(new AniListStudio(858L, "WIT Studio", true))),
                "SPRING",
                2013,
                new AniListCharacterConnection(List.of(
                        edge(40882L, "Eren Yeager"),
                        edge(40881L, "Mikasa Ackerman"))),
                List.of(new AniListTag("Time Travel", 94)));
    }

    private static AniListCharacterEdge edge(Long id, String name) {
        return new AniListCharacterEdge(
                "MAIN",
                new AniListCharacter(
                        id,
                        new AniListCharacterName(name, null),
                        new AniListCharacterImage(null, null)));
    }
}
