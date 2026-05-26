package com.dondeanime.backend.studio;

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

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;
import com.dondeanime.backend.admin.auth.AdminJwtService;
import com.dondeanime.backend.config.SecurityConfig;

@WebMvcTest(StudioController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "admin.username=admin",
        "admin.password=secret",
        "admin.cors.allowed-origins=http://localhost:4321"
})
class StudioControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private StudioRepository studioRepository;

    @MockitoBean
    private AnimeRepository animeRepository;

    @MockitoBean
    private AdminJwtService adminJwtService;

    @Test
    void listReturnsStudiosWithCounts() throws Exception {
        Studio studio = studio();
        when(studioRepository.aggregateStudios()).thenReturn(List.of(aggregation(studio, 12L)));

        mvc.perform(get("/api/studios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("wit-studio"))
                .andExpect(jsonPath("$[0].name").value("WIT Studio"))
                .andExpect(jsonPath("$[0].animationStudio").value(true))
                .andExpect(jsonPath("$[0].animeCount").value(12));
    }

    @Test
    void animesByStudioReturnsSummaries() throws Exception {
        Anime anime = anime();
        when(animeRepository.findByStudioSlug("wit-studio")).thenReturn(List.of(anime));

        mvc.perform(get("/api/studios/wit-studio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("attack-on-titan"))
                .andExpect(jsonPath("$[0].titleEnglish").value("Attack on Titan"))
                .andExpect(jsonPath("$[0].id").doesNotExist())
                .andExpect(jsonPath("$[0].tmdbId").doesNotExist());
    }

    private static Studio studio() {
        Studio studio = new Studio();
        studio.setId(1L);
        studio.setAnilistId(858L);
        studio.setName("WIT Studio");
        studio.setSlug("wit-studio");
        studio.setAnimationStudio(true);
        return studio;
    }

    private static Anime anime() {
        Anime anime = new Anime();
        anime.setId(1L);
        anime.setAnilistId(16498L);
        anime.setSlug("attack-on-titan");
        anime.setTitleEnglish("Attack on Titan");
        anime.setTitleRomaji("Shingeki no Kyojin");
        anime.setFormat("TV");
        anime.setStatus("FINISHED");
        return anime;
    }

    private static StudioRepository.StudioAggregation aggregation(Studio studio, Long count) {
        return new StudioRepository.StudioAggregation() {
            @Override
            public Studio getStudio() {
                return studio;
            }

            @Override
            public Long getAnimeCount() {
                return count;
            }
        };
    }
}
