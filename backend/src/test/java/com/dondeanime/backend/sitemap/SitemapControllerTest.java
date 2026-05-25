package com.dondeanime.backend.sitemap;

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

import com.dondeanime.backend.anime.AnimeRepository;
import com.dondeanime.backend.config.SecurityConfig;
import com.dondeanime.backend.provider.WatchProviderRepository;

@WebMvcTest(SitemapController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "admin.username=admin",
        "admin.password=secret",
        "admin.cors.allowed-origins=http://localhost:4321"
})
class SitemapControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AnimeRepository animeRepository;

    @MockitoBean
    private WatchProviderRepository providerRepository;

    @Test
    void sitemapReturnsAllRouteInputs() throws Exception {
        when(animeRepository.findAllSlugs()).thenReturn(List.of("attack-on-titan", "frieren-beyond-journeys-end"));
        when(providerRepository.aggregateProviderCountries()).thenReturn(List.of(
                providerCountry("Crunchyroll", "ES")));
        when(animeRepository.aggregateGenres()).thenReturn(List.of(
                genre("Slice of Life", 5L)));
        when(animeRepository.aggregateSeasons()).thenReturn(List.of(
                season(2024, "SPRING", 18L)));

        mvc.perform(get("/api/sitemap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animeSlugs[0]").value("attack-on-titan"))
                .andExpect(jsonPath("$.providers[0].slug").value("crunchyroll"))
                .andExpect(jsonPath("$.providers[0].country").value("ES"))
                .andExpect(jsonPath("$.genreSlugs[0]").value("slice-of-life"))
                .andExpect(jsonPath("$.seasons[0].year").value(2024))
                .andExpect(jsonPath("$.seasons[0].season").value("SPRING"));
    }

    @Test
    void unknownSitemapPathReturns404() throws Exception {
        mvc.perform(get("/api/sitemap/extra"))
                .andExpect(status().isNotFound());
    }

    private static WatchProviderRepository.ProviderCountryAggregation providerCountry(
            String providerName,
            String countryCode) {
        return new WatchProviderRepository.ProviderCountryAggregation() {
            @Override
            public String getProviderName() {
                return providerName;
            }

            @Override
            public String getCountryCode() {
                return countryCode;
            }
        };
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
}
