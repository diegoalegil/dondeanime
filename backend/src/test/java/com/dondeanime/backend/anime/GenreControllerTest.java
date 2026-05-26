package com.dondeanime.backend.anime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dondeanime.backend.config.SecurityConfig;

@WebMvcTest(GenreController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "admin.username=admin",
        "admin.password=secret",
        "admin.cors.allowed-origins=http://localhost:4321"
})
class GenreControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AnimeRepository animeRepository;

    @MockitoBean
    private AnimeOverrideService overrideService;

    @Test
    void beginnerEndpointReturnsTopTenAnimeWithEditorialRecommendation() throws Exception {
        Anime first = anime(1L, "attack-on-titan", "Attack on Titan");
        Anime second = anime(2L, "frieren", "Frieren");
        AnimeOverride override = override(first, "Ideal para empezar si quieres accion clara.");

        when(animeRepository.findByGenreSlug("action")).thenReturn(List.of(first, second));
        when(overrideService.findSpanishOverrides(first)).thenReturn(List.of(override));
        when(overrideService.findSpanishOverrides(second)).thenReturn(List.of());

        mvc.perform(get("/api/genres/action/beginner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].anime.slug").value("attack-on-titan"))
                .andExpect(jsonPath("$[0].beginnerRecommendation").value("Ideal para empezar si quieres accion clara."))
                .andExpect(jsonPath("$[1].anime.slug").value("frieren"))
                .andExpect(jsonPath("$[1].beginnerRecommendation").doesNotExist());
    }

    private static Anime anime(Long id, String slug, String title) {
        Anime anime = new Anime();
        anime.setId(id);
        anime.setAnilistId(1000L + id);
        anime.setSlug(slug);
        anime.setTitleEnglish(title);
        anime.setTitleRomaji(title);
        anime.setFormat("TV");
        anime.setStatus("FINISHED");
        anime.setEpisodes(12);
        anime.setEpisodeDuration(24);
        anime.setPopularity(100);
        anime.setCoverImage("https://example.com/cover.jpg");
        anime.setGenres(new LinkedHashSet<>(List.of("Action")));
        return anime;
    }

    private static AnimeOverride override(Anime anime, String fieldValue) {
        AnimeOverride override = new AnimeOverride();
        override.setAnime(anime);
        override.setFieldName("beginner_recommendation");
        override.setFieldValue(fieldValue);
        override.setLocale("es");
        override.setUpdatedAt(Instant.now());
        override.setUpdatedBy("admin");
        return override;
    }
}
