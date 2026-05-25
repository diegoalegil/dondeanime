package com.dondeanime.backend.search;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeSummaryDto;
import com.dondeanime.backend.config.RateLimitFilter;
import com.dondeanime.backend.config.SecurityConfig;

@WebMvcTest(SearchController.class)
@Import({SecurityConfig.class, RateLimitFilter.class})
@TestPropertySource(properties = {
        "admin.username=admin",
        "admin.password=secret",
        "admin.cors.allowed-origins=http://localhost:4321"
})
class SearchControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AnimeSearchService searchService;

    @Test
    void searchAtaqueReturnsAttackOnTitan() throws Exception {
        when(searchService.search("ataque", 10)).thenReturn(List.of(AnimeSummaryDto.from(attackOnTitan())));

        mvc.perform(get("/api/search?q=ataque&limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("attack-on-titan"))
                .andExpect(jsonPath("$[0].titleEnglish").value("Attack on Titan"))
                .andExpect(jsonPath("$[0].id").doesNotExist())
                .andExpect(jsonPath("$[0].tmdbId").doesNotExist());
    }

    @Test
    void searchRejectsThirtyFirstRequestFromSameIp() throws Exception {
        when(searchService.search("ataque", 10)).thenReturn(List.of());

        for (int i = 0; i < 30; i++) {
            mvc.perform(get("/api/search?q=ataque&limit=10")
                    .header("X-Forwarded-For", "198.51.100.10"))
                    .andExpect(status().isOk());
        }

        mvc.perform(get("/api/search?q=ataque&limit=10")
                .header("X-Forwarded-For", "198.51.100.10"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    private static Anime attackOnTitan() {
        Anime anime = new Anime();
        anime.setAnilistId(16498L);
        anime.setSlug("attack-on-titan");
        anime.setTitleEnglish("Attack on Titan");
        anime.setTitleRomaji("Shingeki no Kyojin");
        anime.setFormat("TV");
        anime.setStatus("FINISHED");
        anime.setPopularity(999);
        anime.setCoverImage("https://example.com/cover.jpg");
        return anime;
    }
}
