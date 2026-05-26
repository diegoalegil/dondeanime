package com.dondeanime.backend.anime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dondeanime.backend.admin.auth.AdminJwtService;
import com.dondeanime.backend.config.SecurityConfig;

@WebMvcTest(StudioController.class)
@Import({SecurityConfig.class, AdminJwtService.class})
@TestPropertySource(properties = {
        "admin.username=admin",
        "admin.password=secret",
        "admin.cors.allowed-origins=http://localhost:4321",
        "alerts.jwt-secret=test-jwt-secret"
})
class StudioControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AnimeRepository animeRepository;

    @Test
    void listReturnsStudioSummariesWithSlug() throws Exception {
        when(animeRepository.aggregateStudios()).thenReturn(List.of(aggregation("Madhouse", 2L)));

        mvc.perform(get("/api/studios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Madhouse"))
                .andExpect(jsonPath("$[0].slug").value("madhouse"))
                .andExpect(jsonPath("$[0].animeCount").value(2));
    }

    @Test
    void bestByStudioReturnsMatchingAnimeOrderedByPopularity() throws Exception {
        Anime lower = anime("lower", "Lower", "Madhouse", 10);
        Anime higher = anime("higher", "Higher", "Madhouse", 100);
        Anime other = anime("other", "Other", "Bones", 1000);
        when(animeRepository.findAll()).thenReturn(List.of(lower, other, higher));

        mvc.perform(get("/api/studios/madhouse/best"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].slug").value("higher"))
                .andExpect(jsonPath("$[0].studio").value("Madhouse"))
                .andExpect(jsonPath("$[1].slug").value("lower"));
    }

    private static AnimeRepository.StudioAggregation aggregation(String studio, Long count) {
        return new AnimeRepository.StudioAggregation() {
            @Override
            public String getStudio() {
                return studio;
            }

            @Override
            public Long getAnimeCount() {
                return count;
            }
        };
    }

    private static Anime anime(String slug, String title, String studio, int popularity) {
        Anime anime = new Anime();
        anime.setId((long) popularity);
        anime.setAnilistId((long) popularity);
        anime.setSlug(slug);
        anime.setTitleEnglish(title);
        anime.setTitleRomaji(title);
        anime.setFormat("TV");
        anime.setStatus("FINISHED");
        anime.setEpisodes(12);
        anime.setEpisodeDuration(24);
        anime.setStudio(studio);
        anime.setPopularity(popularity);
        anime.setCoverImage("https://example.com/cover.jpg");
        return anime;
    }
}
