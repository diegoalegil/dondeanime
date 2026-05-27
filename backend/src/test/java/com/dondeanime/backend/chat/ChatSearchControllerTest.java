package com.dondeanime.backend.chat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dondeanime.backend.admin.auth.AdminJwtService;
import com.dondeanime.backend.anime.AnimeSummaryDto;
import com.dondeanime.backend.config.SecurityConfig;

@WebMvcTest(ChatSearchController.class)
@Import({
        SecurityConfig.class,
        AdminJwtService.class
})
@TestPropertySource(properties = {
        "admin.username=admin",
        "admin.password=secret",
        "admin.cors.allowed-origins=http://localhost:4321",
        "alerts.jwt-secret=test-jwt-secret"
})
class ChatSearchControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ChatSearchService chatSearchService;

    @Test
    void searchReturnsRecommendations() throws Exception {
        when(chatSearchService.search(any())).thenReturn(new ChatSearchResponse(
                "He encontrado 1 recomendaciones del catalogo de DondeAnime.",
                List.of(new ChatRecommendationDto(
                        anime(),
                        "https://dondeanime.com/anime/attack-on-titan",
                        "Coincide con tu busqueda."))));

        mvc.perform(post("/api/chat/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"algo oscuro en Crunchyroll","countryCode":"ES"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations.length()").value(1))
                .andExpect(jsonPath("$.recommendations[0].anime.slug").value("attack-on-titan"))
                .andExpect(jsonPath("$.recommendations[0].canonicalUrl")
                        .value("https://dondeanime.com/anime/attack-on-titan"))
                .andExpect(jsonPath("$.recommendations[0].anime.id").doesNotExist());
    }

    private static AnimeSummaryDto anime() {
        return new AnimeSummaryDto(
                16498L,
                "attack-on-titan",
                "Attack on Titan",
                "Shingeki no Kyojin",
                "TV",
                "FINISHED",
                25,
                24,
                "Wit Studio",
                2013,
                85,
                999,
                "https://example.com/cover.jpg",
                Set.of("Action"),
                "SPRING",
                2013);
    }
}
