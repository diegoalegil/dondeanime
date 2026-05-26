package com.dondeanime.backend.anime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.dondeanime.backend.AbstractIntegrationTest;

@DataJpaTest
class AnimeRepositoryTagsTest extends AbstractIntegrationTest {

    @Autowired
    private AnimeRepository animeRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void findByIdWithTagsInitializesTagsForRecommendations() {
        Anime anime = anime();
        anime.setTags(Set.of(
                new AnimeTag("Time Travel", 94),
                new AnimeTag("Alternate Universe", 71)));
        Anime saved = animeRepository.saveAndFlush(anime);
        entityManager.clear();

        Anime found = animeRepository.findByIdWithTags(saved.getId()).orElseThrow();

        assertThat(entityManagerFactory.getPersistenceUnitUtil().isLoaded(found, "tags"))
                .isTrue();
        assertThat(found.getTags())
                .containsExactlyInAnyOrder(
                        new AnimeTag("Time Travel", 94),
                        new AnimeTag("Alternate Universe", 71));
    }

    private static Anime anime() {
        Anime anime = new Anime();
        anime.setAnilistId(30276L);
        anime.setSlug("one-punch-man");
        anime.setTitleEnglish("One Punch Man");
        anime.setTitleRomaji("One Punch Man");
        anime.setDescription("Hero for fun");
        anime.setFormat("TV");
        anime.setStatus("FINISHED");
        return anime;
    }
}
