package com.dondeanime.backend.anime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class AnimeOverrideServiceTest {

    private final AnimeOverrideRepository repository = mock(AnimeOverrideRepository.class);
    private final AnimeOverrideService service = new AnimeOverrideService(repository);

    @Test
    void saveOverrideAllowsBeginnerRecommendationAlias() {
        Anime anime = anime();
        when(repository.findByAnime_IdAndFieldNameAndLocale(1L, "beginner_recommendation", "es"))
                .thenReturn(Optional.empty());
        when(repository.save(any(AnimeOverride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AnimeOverride override = service.saveOverride(
                anime,
                "beginnerRecommendation",
                "Buena primera serie si buscas accion directa.",
                "ES",
                "admin");

        assertThat(override.getFieldName()).isEqualTo("beginner_recommendation");
        assertThat(override.getFieldValue()).isEqualTo("Buena primera serie si buscas accion directa.");
        assertThat(override.getLocale()).isEqualTo("es");
        verify(repository).save(override);
    }

    @Test
    void originalValueForBeginnerRecommendationIsEmptyBecauseItIsEditorialOnly() {
        assertThat(service.originalValue(anime(), "beginner_recommendation")).isNull();
    }

    private static Anime anime() {
        Anime anime = new Anime();
        anime.setId(1L);
        anime.setSlug("attack-on-titan");
        anime.setTitleEnglish("Attack on Titan");
        anime.setTitleRomaji("Shingeki no Kyojin");
        anime.setDescription("Descripcion AniList");
        return anime;
    }
}
