package com.dondeanime.backend.embedding;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dondeanime.backend.admin.auth.AdminJwtService;
import com.dondeanime.backend.config.SecurityConfig;

@WebMvcTest(EmbeddingAdminController.class)
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
class EmbeddingAdminControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EmbeddingRebuildService rebuildService;

    @Autowired
    private AdminJwtService adminJwtService;

    @Test
    void rebuildRequiresAdminToken() throws Exception {
        mvc.perform(post("/api/admin/embeddings/rebuild"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rebuildReturnsPreparedDocumentCount() throws Exception {
        when(rebuildService.rebuild()).thenReturn(new EmbeddingRebuildResponse(2, 1, 1, "test-model"));

        mvc.perform(post("/api/admin/embeddings/rebuild")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentsPrepared").value(2))
                .andExpect(jsonPath("$.embeddingsUpdated").value(1))
                .andExpect(jsonPath("$.embeddingsSkipped").value(1))
                .andExpect(jsonPath("$.model").value("test-model"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.tmdbId").doesNotExist())
                .andExpect(jsonPath("$.syncedAt").doesNotExist());
    }

    private String bearerToken() {
        return "Bearer " + adminJwtService.createAdminSession().token();
    }
}
