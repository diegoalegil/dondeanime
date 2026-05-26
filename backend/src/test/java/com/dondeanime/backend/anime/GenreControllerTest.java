package com.dondeanime.backend.anime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dondeanime.backend.admin.auth.AdminJwtService;
import com.dondeanime.backend.config.SecurityConfig;

@WebMvcTest(GenreController.class)
@Import({SecurityConfig.class, AdminJwtService.class})
@TestPropertySource(properties = {
        "admin.username=admin",
        "admin.password=secret",
        "admin.cors.allowed-origins=http://localhost:4321",
        "alerts.jwt-secret=test-jwt-secret"
})
class GenreControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AnimeRepository animeRepository;

    @Test
    void listReturnsGenreSummaries() throws Exception {
        when(animeRepository.aggregateGenres()).thenReturn(List.of(
                genre("Action", 12L),
                genre("Slice of Life", 5L)));

        mvc.perform(get("/api/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].slug").value("action"))
                .andExpect(jsonPath("$[0].name").value("Action"))
                .andExpect(jsonPath("$[0].animeCount").value(12))
                .andExpect(jsonPath("$[1].slug").value("slice-of-life"));
    }

    @Test
    void animesByGenreReturnsSummaries() throws Exception {
        Anime anime = anime("frieren-beyond-journeys-end", "Frieren: Beyond Journey's End");
        when(animeRepository.findByGenreSlug("slice-of-life")).thenReturn(List.of(anime));

        mvc.perform(get("/api/genres/Slice-Of-Life"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("frieren-beyond-journeys-end"))
                .andExpect(jsonPath("$[0].titleEnglish").value("Frieren: Beyond Journey's End"))
                .andExpect(jsonPath("$[0].genres[0]").value("Slice of Life"));
    }

    private static AnimeRepository.GenreAggregation genre(String name, Long animeCount) {
        return new AnimeRepository.GenreAggregation() {
            @Override
            public String getGenre() {
                return name;
            }

            @Override
            public Long getAnimeCount() {
                return animeCount;
            }
        };
    }

    private static Anime anime(String slug, String titleEnglish) {
        Anime anime = new Anime();
        anime.setId(1L);
        anime.setAnilistId(154587L);
        anime.setSlug(slug);
        anime.setTitleEnglish(titleEnglish);
        anime.setTitleRomaji("Sousou no Frieren");
        anime.setFormat("TV");
        anime.setStatus("FINISHED");
        anime.setGenres(Set.of("Slice of Life"));
        anime.setStartYear(2023);
        anime.setSeason("FALL");
        anime.setSeasonYear(2023);
        return anime;
    }
}
