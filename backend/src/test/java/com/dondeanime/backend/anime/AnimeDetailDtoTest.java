package com.dondeanime.backend.anime;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.dondeanime.backend.character.AnimeCharacter;
import com.dondeanime.backend.character.AnimeCharacterRole;
import com.dondeanime.backend.studio.Studio;

class AnimeDetailDtoTest {

    @Test
    void spanishOverrideWinsOverAnilistValue() {
        Anime anime = anime();

        AnimeOverride descriptionOverride = override(anime, "description", "Descripcion propia");
        AnimeOverride titleOverride = override(anime, "title_english", "Titulo propio");

        AnimeDetailDto dto = AnimeDetailDto.from(anime, List.of(descriptionOverride, titleOverride));

        assertThat(dto.description()).isEqualTo("Descripcion propia");
        assertThat(dto.descriptionTranslationPending()).isFalse();
        assertThat(dto.titleEnglish()).isEqualTo("Titulo propio");
        assertThat(dto.titleRomaji()).isEqualTo("Shingeki no Kyojin");
    }

    @Test
    void fallbackUsesAnilistValueWhenNoOverrideExists() {
        Anime anime = anime();

        AnimeDetailDto dto = AnimeDetailDto.from(anime, List.of());

        assertThat(dto.description()).isEqualTo("Descripcion AniList");
        assertThat(dto.descriptionTranslationPending()).isTrue();
        assertThat(dto.titleEnglish()).isEqualTo("Attack on Titan");
        assertThat(dto.titleRomaji()).isEqualTo("Shingeki no Kyojin");
        assertThat(dto.trailerYoutubeId()).isEqualTo("abc123DEF45");
    }

    @Test
    void spanishDescriptionWinsOverAnilistValue() {
        Anime anime = anime();
        anime.setDescriptionEs("Descripcion TMDb en espanol");

        AnimeDetailDto dto = AnimeDetailDto.from(anime, List.of());

        assertThat(dto.description()).isEqualTo("Descripcion TMDb en espanol");
        assertThat(dto.descriptionTranslationPending()).isFalse();
    }

    @Test
    void ignoresOverridesFromOtherLocale() {
        Anime anime = anime();
        AnimeOverride override = override(anime, "description", "English override");
        override.setLocale("en");

        AnimeDetailDto dto = AnimeDetailDto.from(anime, List.of(override));

        assertThat(dto.description()).isEqualTo("Descripcion AniList");
        assertThat(dto.descriptionTranslationPending()).isTrue();
    }

    @Test
    void includesPublicStudioData() {
        Anime anime = anime();
        Studio studio = new Studio();
        studio.setAnilistId(858L);
        studio.setName("WIT Studio");
        studio.setSlug("wit-studio");
        studio.setAnimationStudio(true);
        anime.setStudios(Set.of(studio));

        AnimeDetailDto dto = AnimeDetailDto.from(anime, List.of());

        assertThat(dto.studios()).hasSize(1);
        assertThat(dto.studios().getFirst().slug()).isEqualTo("wit-studio");
        assertThat(dto.studios().getFirst().name()).isEqualTo("WIT Studio");
        assertThat(dto.studios().getFirst().animationStudio()).isTrue();
    }

    @Test
    void includesPublicCharacterData() {
        Anime anime = anime();
        anime.replaceCharacterRoles(List.of(characterRole(40882L, "Eren Yeager", "https://img.example/eren.jpg")));

        AnimeDetailDto dto = AnimeDetailDto.from(anime, List.of());

        assertThat(dto.characters()).hasSize(1);
        assertThat(dto.characters().getFirst().anilistId()).isEqualTo(40882L);
        assertThat(dto.characters().getFirst().name()).isEqualTo("Eren Yeager");
        assertThat(dto.characters().getFirst().image()).isEqualTo("https://img.example/eren.jpg");
        assertThat(dto.characters().getFirst().role()).isEqualTo("MAIN");
    }

    private static Anime anime() {
        Anime anime = new Anime();
        anime.setId(1L);
        anime.setAnilistId(16498L);
        anime.setSlug("attack-on-titan");
        anime.setTitleEnglish("Attack on Titan");
        anime.setTitleRomaji("Shingeki no Kyojin");
        anime.setTrailerYoutubeId("abc123DEF45");
        anime.setDescription("Descripcion AniList");
        anime.setFormat("TV");
        anime.setStatus("FINISHED");
        return anime;
    }

    private static AnimeOverride override(Anime anime, String fieldName, String fieldValue) {
        AnimeOverride override = new AnimeOverride();
        override.setAnime(anime);
        override.setFieldName(fieldName);
        override.setFieldValue(fieldValue);
        override.setLocale("es");
        override.setUpdatedAt(Instant.now());
        override.setUpdatedBy("admin");
        return override;
    }

    private static AnimeCharacterRole characterRole(Long anilistId, String name, String image) {
        AnimeCharacter character = new AnimeCharacter();
        character.setAnilistId(anilistId);
        character.setName(name);
        character.setImage(image);

        AnimeCharacterRole role = new AnimeCharacterRole();
        role.setCharacter(character);
        role.setRole("MAIN");
        return role;
    }
}
