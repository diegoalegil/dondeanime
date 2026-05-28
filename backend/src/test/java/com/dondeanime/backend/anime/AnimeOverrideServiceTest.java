package com.dondeanime.backend.anime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class AnimeOverrideServiceTest {

    private final AnimeOverrideRepository repository = mock(AnimeOverrideRepository.class);
    private final AnimeOverrideService service = new AnimeOverrideService(repository);

    @Test
    void saveOverrideNormalizesFieldLocaleAndValue() {
        Anime anime = anime();
        when(repository.findByAnime_IdAndFieldNameAndLocale(1L, "title_english", "es"))
                .thenReturn(Optional.empty());
        when(repository.save(any(AnimeOverride.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AnimeOverride saved = service.saveOverride(
                anime,
                " titleEnglish ",
                " Titulo propio ",
                null,
                "admin");

        assertThat(saved.getAnime()).isSameAs(anime);
        assertThat(saved.getFieldName()).isEqualTo("title_english");
        assertThat(saved.getFieldValue()).isEqualTo("Titulo propio");
        assertThat(saved.getLocale()).isEqualTo("es");
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getUpdatedBy()).isEqualTo("admin");
    }

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
    void deleteOverrideDeletesExistingValue() {
        Anime anime = anime();
        AnimeOverride override = new AnimeOverride();
        when(repository.findByAnime_IdAndFieldNameAndLocale(1L, "description", "es"))
                .thenReturn(Optional.of(override));

        service.deleteOverride(anime, "description", "ES");

        verify(repository).delete(override);
    }

    @Test
    void originalValueReturnsSelectedField() {
        Anime anime = anime();

        assertThat(service.originalValue(anime, "description")).isEqualTo("Descripcion AniList");
        assertThat(service.originalValue(anime, "title_english")).isEqualTo("Attack on Titan");
        assertThat(service.originalValue(anime, "titleRomaji")).isEqualTo("Shingeki no Kyojin");
    }

    @Test
    void originalValueForBeginnerRecommendationIsEmptyBecauseItIsEditorialOnly() {
        assertThat(service.originalValue(anime(), "beginner_recommendation")).isNull();
    }

    @Test
    void invalidOverrideInputThrows400() {
        assertThatThrownBy(() -> AnimeOverrideService.normalizeFieldName("popularity"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
        assertThatThrownBy(() -> AnimeOverrideService.normalizeLocale("demasiado-largo"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
        assertThatThrownBy(() -> service.saveOverride(anime(), "description", " ", "es", "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
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
