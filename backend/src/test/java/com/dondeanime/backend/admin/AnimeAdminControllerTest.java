package com.dondeanime.backend.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeMatchingService;
import com.dondeanime.backend.anime.AnimeOverride;
import com.dondeanime.backend.anime.AnimeOverrideService;
import com.dondeanime.backend.anime.AnimeRepository;
import com.dondeanime.backend.config.SecurityConfig;

@WebMvcTest(AnimeAdminController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "admin.username=admin",
        "admin.password=secret",
        "admin.cors.allowed-origins=http://localhost:4321"
})
class AnimeAdminControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AnimeRepository animeRepository;

    @MockitoBean
    private AnimeOverrideService overrideService;

    @MockitoBean
    private AnimeMatchingService matchingService;

    @Test
    void adminEndpointRequiresBasicAuth() throws Exception {
        mvc.perform(post("/api/admin/anime/attack-on-titan/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fieldName":"description","fieldValue":"Descripción propia","locale":"es"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postOverrideWithCredentialsReturnsRefreshedDetail() throws Exception {
        Anime anime = anime();
        AnimeOverride override = override(anime, "description", "Descripción propia");

        when(animeRepository.findBySlug("attack-on-titan")).thenReturn(Optional.of(anime));
        when(overrideService.findSpanishOverrides(anime)).thenReturn(List.of(override));

        mvc.perform(post("/api/admin/anime/attack-on-titan/override")
                        .with(httpBasic("admin", "secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fieldName":"description","fieldValue":"Descripción propia","locale":"es"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("attack-on-titan"))
                .andExpect(jsonPath("$.description").value("Descripción propia"));

        verify(overrideService).saveOverride(
                eq(anime),
                eq("description"),
                eq("Descripción propia"),
                eq("es"),
                eq("admin"));
    }

    @Test
    void deleteOverrideWithCredentialsReturnsFallbackDetail() throws Exception {
        Anime anime = anime();

        when(animeRepository.findBySlug("attack-on-titan")).thenReturn(Optional.of(anime));
        when(overrideService.findSpanishOverrides(anime)).thenReturn(List.of());

        mvc.perform(delete("/api/admin/anime/attack-on-titan/override")
                        .queryParam("field", "description")
                        .queryParam("locale", "es")
                        .with(httpBasic("admin", "secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Descripción AniList"));

        verify(overrideService).deleteOverride(anime, "description", "es");
    }

    @Test
    void getOverridesReturnsActiveOverridesWithOriginalValue() throws Exception {
        Anime anime = anime();
        AnimeOverride override = override(anime, "title_english", "Título propio");

        when(animeRepository.findBySlug("attack-on-titan")).thenReturn(Optional.of(anime));
        when(overrideService.listOverrides(anime)).thenReturn(List.of(override));
        when(overrideService.originalValue(any(), eq("title_english"))).thenReturn("Attack on Titan");

        mvc.perform(get("/api/admin/anime/attack-on-titan/overrides")
                        .with(httpBasic("admin", "secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fieldName").value("title_english"))
                .andExpect(jsonPath("$[0].fieldValue").value("Título propio"))
                .andExpect(jsonPath("$[0].originalValue").value("Attack on Titan"));
    }

    @Test
    void rematchWithCredentialsReturnsResult() throws Exception {
        when(matchingService.rematch("attack-on-titan"))
                .thenReturn(Optional.of(new AnimeMatchingService.RematchResult("attack-on-titan", true)));

        mvc.perform(post("/api/admin/anime/attack-on-titan/rematch")
                        .with(httpBasic("admin", "secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("attack-on-titan"))
                .andExpect(jsonPath("$.matched").value(true));

        verify(matchingService).rematch("attack-on-titan");
    }

    @Test
    void rematchUnknownSlugReturns404() throws Exception {
        when(matchingService.rematch("inexistente")).thenReturn(Optional.empty());

        mvc.perform(post("/api/admin/anime/inexistente/rematch")
                        .with(httpBasic("admin", "secret")))
                .andExpect(status().isNotFound());
    }

    private static Anime anime() {
        Anime anime = new Anime();
        anime.setId(1L);
        anime.setAnilistId(16498L);
        anime.setSlug("attack-on-titan");
        anime.setTitleEnglish("Attack on Titan");
        anime.setTitleRomaji("Shingeki no Kyojin");
        anime.setDescription("Descripción AniList");
        anime.setFormat("TV");
        anime.setStatus("FINISHED");
        return anime;
    }

    private static AnimeOverride override(Anime anime, String fieldName, String fieldValue) {
        AnimeOverride override = new AnimeOverride();
        override.setAnime(anime);
        override.setFieldName(fieldName);
        override.setFieldValue(fieldValue);
        override.setLocale("es");
        override.setUpdatedAt(Instant.now());
        override.setUpdatedBy("admin");
        return override;
    }
}
