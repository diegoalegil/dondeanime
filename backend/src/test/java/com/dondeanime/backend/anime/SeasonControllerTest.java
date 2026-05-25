package com.dondeanime.backend.anime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

import com.dondeanime.backend.config.SecurityConfig;

@WebMvcTest(SeasonController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "admin.username=admin",
        "admin.password=secret",
        "admin.cors.allowed-origins=http://localhost:4321"
})
class SeasonControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AnimeRepository animeRepository;

    @Test
    void listReturnsSeasonSummaries() throws Exception {
        when(animeRepository.aggregateSeasons()).thenReturn(List.of(
                season(2024, "SPRING", 18L),
                season(2024, "WINTER", 16L)));

        mvc.perform(get("/api/seasons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].year").value(2024))
                .andExpect(jsonPath("$[0].season").value("SPRING"))
                .andExpect(jsonPath("$[0].animeCount").value(18));
    }

    @Test
    void animesBySeasonReturnsSummariesAndNormalizesSeason() throws Exception {
        Anime anime = anime("kaiju-no-8", "Kaiju No. 8");
        when(animeRepository.findBySeasonYearAndSeason(2024, "SPRING")).thenReturn(List.of(anime));

        mvc.perform(get("/api/seasons/2024/spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("kaiju-no-8"))
                .andExpect(jsonPath("$[0].season").value("SPRING"))
                .andExpect(jsonPath("$[0].seasonYear").value(2024));

        verify(animeRepository).findBySeasonYearAndSeason(2024, "SPRING");
    }

    @Test
    void animesBySeasonWithInvalidSeasonReturns400() throws Exception {
        mvc.perform(get("/api/seasons/2024/INVALID"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(animeRepository);
    }

    private static AnimeRepository.SeasonAggregation season(Integer year, String season, Long animeCount) {
        return new AnimeRepository.SeasonAggregation() {
            @Override
            public Integer getYear() {
                return year;
            }

            @Override
            public String getSeason() {
                return season;
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
        anime.setAnilistId(170942L);
        anime.setSlug(slug);
        anime.setTitleEnglish(titleEnglish);
        anime.setTitleRomaji("Kaijuu 8-gou");
        anime.setFormat("TV");
        anime.setStatus("FINISHED");
        anime.setGenres(Set.of("Action"));
        anime.setStartYear(2024);
        anime.setSeason("SPRING");
        anime.setSeasonYear(2024);
        return anime;
    }
}
