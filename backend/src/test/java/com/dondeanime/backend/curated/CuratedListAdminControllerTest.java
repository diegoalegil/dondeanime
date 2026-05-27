package com.dondeanime.backend.curated;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.dondeanime.backend.admin.auth.AdminJwtService;
import com.dondeanime.backend.config.SecurityConfig;

@WebMvcTest(CuratedListAdminController.class)
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
class CuratedListAdminControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CuratedListService service;

    @Autowired
    private AdminJwtService adminJwtService;

    @Test
    void adminListsRequireAuth() throws Exception {
        mvc.perform(get("/api/admin/lists"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listReturnsAdminDtosWithCredentials() throws Exception {
        when(service.adminLists()).thenReturn(List.of(adminDto("DRAFT")));

        mvc.perform(get("/api/admin/lists")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("anime-para-empezar"))
                .andExpect(jsonPath("$[0].id").doesNotExist());
    }

    @Test
    void saveListWithCredentials() throws Exception {
        when(service.saveAdminList(any(CuratedListSaveRequest.class))).thenReturn(adminDto("DRAFT"));

        mvc.perform(post("/api/admin/lists")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Anime para empezar","description":"Lista curada","owner":"Diego"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("anime-para-empezar"));
    }

    @Test
    void addMoveDeleteAndPublishListItems() throws Exception {
        CuratedListAdminDto draft = adminDto("DRAFT");
        when(service.addAdminItem(any(String.class), any(CuratedListItemSaveRequest.class)))
                .thenReturn(Optional.of(draft));
        when(service.moveAdminItem("anime-para-empezar", "frieren", -1)).thenReturn(Optional.of(draft));
        when(service.deleteAdminItem("anime-para-empezar", "frieren")).thenReturn(Optional.of(draft));
        when(service.publishAdminList("anime-para-empezar")).thenReturn(Optional.of(adminDto("PUBLISHED")));

        mvc.perform(post("/api/admin/lists/anime-para-empezar/items")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"animeSlug":"frieren","note":"Fantasia moderna"}
                                """))
                .andExpect(status().isOk());

        mvc.perform(post("/api/admin/lists/anime-para-empezar/items/frieren/up")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/admin/lists/anime-para-empezar/items/frieren")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk());

        mvc.perform(post("/api/admin/lists/anime-para-empezar/publish")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(service).publishAdminList("anime-para-empezar");
    }

    @Test
    void missingListReturns404() throws Exception {
        when(service.publishAdminList("missing")).thenReturn(Optional.empty());

        mvc.perform(post("/api/admin/lists/missing/publish")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isNotFound());
    }

    private static CuratedListAdminDto adminDto(String status) {
        return new CuratedListAdminDto(
                "anime-para-empezar",
                "Anime para empezar",
                "Lista curada",
                "Diego",
                status.equals("PUBLISHED") ? "PUBLIC" : "PRIVATE",
                status,
                List.of());
    }

    private String bearerToken() {
        return "Bearer " + adminJwtService.createAdminSession().token();
    }
}
