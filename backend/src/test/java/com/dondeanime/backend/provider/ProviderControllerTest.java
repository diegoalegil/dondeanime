package com.dondeanime.backend.provider;

import static org.mockito.Mockito.verify;
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

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;
import com.dondeanime.backend.admin.auth.AdminJwtService;
import com.dondeanime.backend.config.SecurityConfig;

@WebMvcTest(ProviderController.class)
@Import({SecurityConfig.class, AdminJwtService.class})
@TestPropertySource(properties = {
        "admin.username=admin",
        "admin.password=secret",
        "admin.cors.allowed-origins=http://localhost:4321",
        "alerts.jwt-secret=test-jwt-secret"
})
class ProviderControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private WatchProviderRepository providerRepository;

    @MockitoBean
    private AnimeRepository animeRepository;

    @Test
    void listReturnsGlobalProviderSummaries() throws Exception {
        when(providerRepository.aggregateAllProviders()).thenReturn(List.of(
                provider("Crunchyroll", "https://img.example/cr.png", 34L)));

        mvc.perform(get("/api/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("crunchyroll"))
                .andExpect(jsonPath("$[0].providerName").value("Crunchyroll"))
                .andExpect(jsonPath("$[0].logoUrl").value("https://img.example/cr.png"))
                .andExpect(jsonPath("$[0].animeCount").value(34));
    }

    @Test
    void listByCountryNormalizesCountry() throws Exception {
        when(providerRepository.aggregateProvidersByCountry("ES")).thenReturn(List.of(
                provider("Netflix", "https://img.example/netflix.png", 9L)));

        mvc.perform(get("/api/providers").queryParam("country", "es"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("netflix"))
                .andExpect(jsonPath("$[0].animeCount").value(9));

        verify(providerRepository).aggregateProvidersByCountry("ES");
    }

    @Test
    void animesByProviderAndCountryReturnsSummaries() throws Exception {
        Anime anime = anime("attack-on-titan", "Attack on Titan");
        when(animeRepository.findByProviderSlugAndCountry("amazon-prime-video", "MX")).thenReturn(List.of(anime));

        mvc.perform(get("/api/providers/Amazon-Prime-Video/mx"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("attack-on-titan"))
                .andExpect(jsonPath("$[0].titleEnglish").value("Attack on Titan"));

        verify(animeRepository).findByProviderSlugAndCountry("amazon-prime-video", "MX");
    }

    @Test
    void animesByCountryReturnsSummaries() throws Exception {
        Anime anime = anime("one-piece", "One Piece");
        when(animeRepository.findByCountryWithGenres("ES")).thenReturn(List.of(anime));

        mvc.perform(get("/api/providers/country/es/anime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("one-piece"))
                .andExpect(jsonPath("$[0].titleEnglish").value("One Piece"));

        verify(animeRepository).findByCountryWithGenres("ES");
    }

    private static WatchProviderRepository.ProviderAggregation provider(
            String providerName,
            String logoUrl,
            Long animeCount) {
        return new WatchProviderRepository.ProviderAggregation() {
            @Override
            public String getProviderName() {
                return providerName;
            }

            @Override
            public String getLogoUrl() {
                return logoUrl;
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
        anime.setAnilistId(16498L);
        anime.setSlug(slug);
        anime.setTitleEnglish(titleEnglish);
        anime.setTitleRomaji("Shingeki no Kyojin");
        anime.setFormat("TV");
        anime.setStatus("FINISHED");
        anime.setGenres(Set.of("Action"));
        anime.setStartYear(2013);
        anime.setSeason("SPRING");
        anime.setSeasonYear(2013);
        return anime;
    }
}
