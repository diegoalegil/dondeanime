package com.dondeanime.backend.anime;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest(properties = {
        "tmdb.api-key=test",
        "admin.username=admin",
        "admin.password=secret",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class AnimeOverrideRepositoryTest {

    @Autowired
    private AnimeRepository animeRepository;

    @Autowired
    private AnimeOverrideRepository overrideRepository;

    @Test
    void persistAndFindOverrideByAnimeAndLocale() {
        Anime anime = animeRepository.saveAndFlush(anime());

        AnimeOverride override = new AnimeOverride();
        override.setAnime(anime);
        override.setFieldName("description");
        override.setFieldValue("Descripción propia");
        override.setLocale("es");
        override.setUpdatedAt(Instant.now());
        override.setUpdatedBy("admin");

        overrideRepository.saveAndFlush(override);

        List<AnimeOverride> found = overrideRepository
                .findByAnime_IdAndLocaleOrderByFieldNameAsc(anime.getId(), "es");

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getFieldName()).isEqualTo("description");
        assertThat(found.getFirst().getFieldValue()).isEqualTo("Descripción propia");
        assertThat(found.getFirst().getAnime().getId()).isEqualTo(anime.getId());
    }

    @Test
    void findSpecificOverrideByUniqueKey() {
        Anime anime = animeRepository.saveAndFlush(anime());

        AnimeOverride override = new AnimeOverride();
        override.setAnime(anime);
        override.setFieldName("title_english");
        override.setFieldValue("Título propio");
        override.setLocale("es");
        override.setUpdatedAt(Instant.now());
        override.setUpdatedBy("diego");
        overrideRepository.saveAndFlush(override);

        assertThat(overrideRepository.findByAnime_IdAndFieldNameAndLocale(
                anime.getId(),
                "title_english",
                "es"))
                .isPresent()
                .get()
                .extracting(AnimeOverride::getUpdatedBy)
                .isEqualTo("diego");
    }

    private static Anime anime() {
        Anime anime = new Anime();
        anime.setAnilistId(16498L);
        anime.setSlug("attack-on-titan");
        anime.setTitleEnglish("Attack on Titan");
        anime.setTitleRomaji("Shingeki no Kyojin");
        anime.setDescription("Descripción AniList");
        anime.setFormat("TV");
        anime.setStatus("FINISHED");
        return anime;
    }
}
