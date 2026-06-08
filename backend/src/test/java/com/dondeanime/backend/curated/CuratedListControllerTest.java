package com.dondeanime.backend.curated;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dondeanime.backend.admin.auth.AdminJwtService;
import com.dondeanime.backend.anime.AnimeSummaryDto;
import com.dondeanime.backend.config.SecurityConfig;
import com.dondeanime.backend.premium.PremiumAccessService;

@WebMvcTest(CuratedListController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "admin.username=admin",
        "admin.password=secret",
        "admin.cors.allowed-origins=http://localhost:4321"
})
class CuratedListControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CuratedListService service;

    @MockitoBean
    private PremiumAccessService premiumAccessService;

    @MockitoBean
    private AdminJwtService adminJwtService;

    @Test
    void listReturnsPublishedCuratedLists() throws Exception {
        when(service.publishedLists()).thenReturn(List.of(new CuratedListSummaryDto(
                "anime-para-empezar",
                "Anime para empezar",
                "Lista curada.",
                "Diego",
                "PUBLIC",
                "PUBLISHED",
                false,
                2)));

        mvc.perform(get("/api/lists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("anime-para-empezar"))
                .andExpect(jsonPath("$[0].owner").value("Diego"))
                .andExpect(jsonPath("$[0].id").doesNotExist());
    }

    @Test
    void detailReturnsOrderedAnimeAndItemListSchema() throws Exception {
        CuratedListDetailDto detail = detailDto();
        when(service.publishedList("anime-para-empezar", false)).thenReturn(Optional.of(detail));

        mvc.perform(get("/api/lists/anime-para-empezar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("anime-para-empezar"))
                .andExpect(jsonPath("$.items[0].animeSlug").value("frieren-beyond-journeys-end"))
                .andExpect(jsonPath("$.items[0].anime.slug").value("frieren-beyond-journeys-end"))
                .andExpect(jsonPath("$.schema['@type']").value("ItemList"))
                .andExpect(jsonPath("$.schema.itemListElement[0]['@type']").value("ListItem"))
                .andExpect(jsonPath("$.schema.itemListElement[0].url")
                        .value("https://dondeanime.com/anime/frieren-beyond-journeys-end"))
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void detailIgnoresSpoofedPremiumViewerHeader() throws Exception {
        CuratedListDetailDto detail = detailDto(true);
        when(service.publishedList("anime-para-empezar", false)).thenReturn(Optional.of(detail));

        mvc.perform(get("/api/lists/anime-para-empezar")
                        .header("X-User-Email", "premium@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.premiumPreview").value(true));
    }

    @Test
    void detailUsesVerifiedPremiumBearer() throws Exception {
        CuratedListDetailDto detail = detailDto(false);
        when(premiumAccessService.hasActivePremiumAccess("Bearer premium-token")).thenReturn(true);
        when(service.publishedList("anime-para-empezar", true)).thenReturn(Optional.of(detail));

        mvc.perform(get("/api/lists/anime-para-empezar")
                        .header("Authorization", "Bearer premium-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.premiumPreview").value(false));
    }

    @Test
    void detailReturns404WhenListIsNotPublished() throws Exception {
        when(service.publishedList("draft-list", false)).thenReturn(Optional.empty());

        mvc.perform(get("/api/lists/draft-list"))
                .andExpect(status().isNotFound());
    }

    private static CuratedListDetailDto detailDto() {
        return detailDto(false);
    }

    private static CuratedListDetailDto detailDto(boolean premiumPreview) {
        AnimeSummaryDto anime = new AnimeSummaryDto(
                154587L,
                "frieren-beyond-journeys-end",
                "Frieren",
                "Sousou no Frieren",
                "TV",
                "FINISHED",
                28,
                24,
                "Madhouse",
                2023,
                90,
                100_000,
                "https://img.example/frieren.jpg",
                java.util.Set.of("Adventure", "Fantasy"),
                "FALL",
                2023);
        List<CuratedListItemDto> items = List.of(new CuratedListItemDto(
                "frieren-beyond-journeys-end",
                1,
                "Fantasia moderna.",
                anime));
        CuratedList list = new CuratedList();
        list.setSlug("anime-para-empezar");
        list.setTitle("Anime para empezar");
        list.setDescription("Lista curada.");
        list.setOwner("Diego");
        list.setVisibility(CuratedListVisibility.PUBLIC);
        list.setStatus(CuratedListStatus.PUBLISHED);
        list.setPremiumOnly(premiumPreview);
        return CuratedListDetailDto.from(
                list,
                items,
                "https://dondeanime.com",
                premiumPreview,
                premiumPreview ? "https://dondeanime.com/premium" : null);
    }
}
