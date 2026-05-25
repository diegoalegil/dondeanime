package com.dondeanime.backend.anime;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

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

    private static Anime anime() {
        Anime anime = new Anime();
        anime.setId(1L);
        anime.setAnilistId(16498L);
        anime.setSlug("attack-on-titan");
        anime.setTitleEnglish("Attack on Titan");
        anime.setTitleRomaji("Shingeki no Kyojin");
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
}
