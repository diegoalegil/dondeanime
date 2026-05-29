package com.dondeanime.backend.curated;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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

import com.dondeanime.backend.admin.auth.AdminJwtService;
import com.dondeanime.backend.config.SecurityConfig;

@WebMvcTest(CuratorAdminController.class)
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
class CuratorAdminControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CuratorProfileService service;

    @Autowired
    private AdminJwtService adminJwtService;

    @Test
    void curatorsRequireAuth() throws Exception {
        mvc.perform(get("/api/admin/curators"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listApprovedCurators() throws Exception {
        when(service.listCurators()).thenReturn(List.of(dto(true)));

        mvc.perform(get("/api/admin/curators")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("diego@example.com"))
                .andExpect(jsonPath("$[0].approved").value(true));
    }

    @Test
    void approveCuratorWithCredentials() throws Exception {
        when(service.approveCurator(any(CuratorProfileSaveRequest.class))).thenReturn(dto(true));

        mvc.perform(post("/api/admin/curators")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"Diego@Example.com","displayName":"Diego"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("diego@example.com"))
                .andExpect(jsonPath("$.approved").value(true));
    }

    @Test
    void revokeCuratorWithCredentials() throws Exception {
        when(service.revokeCurator("diego@example.com")).thenReturn(Optional.of(dto(false)));

        mvc.perform(post("/api/admin/curators/diego@example.com/revoke")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(false));
    }

    @Test
    void missingCuratorReturns404() throws Exception {
        when(service.revokeCurator("missing@example.com")).thenReturn(Optional.empty());

        mvc.perform(post("/api/admin/curators/missing@example.com/revoke")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isNotFound());
    }

    private static CuratorProfileDto dto(boolean approved) {
        return new CuratorProfileDto(
                "diego@example.com",
                "Diego",
                approved,
                Instant.parse("2026-05-27T10:00:00Z"),
                approved ? null : Instant.parse("2026-05-27T11:00:00Z"));
    }

    private String bearerToken() {
        return "Bearer " + adminJwtService.createAdminSession().token();
    }
}
