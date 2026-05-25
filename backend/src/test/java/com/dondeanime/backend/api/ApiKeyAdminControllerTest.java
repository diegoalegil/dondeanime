package com.dondeanime.backend.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dondeanime.backend.config.SecurityConfig;

@WebMvcTest(ApiKeyAdminController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "admin.username=admin",
        "admin.password=secret",
        "admin.cors.allowed-origins=http://localhost:4321"
})
class ApiKeyAdminControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ApiKeyService apiKeyService;

    @Test
    void createsApiKeyWithBasicAuth() throws Exception {
        when(apiKeyService.create(any(ApiKeyCreateRequest.class)))
                .thenReturn(new ApiKeyDto(
                        1L,
                        "da_free_test",
                        "diego@example.com",
                        "FREE",
                        Instant.parse("2026-05-25T12:00:00Z"),
                        null,
                        1_000,
                        0));

        mvc.perform(post("/api/admin/api-keys")
                        .with(httpBasic("admin", "secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerEmail":"diego@example.com","tier":"FREE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("da_free_test"))
                .andExpect(jsonPath("$.monthlyQuota").value(1_000));
    }

    @Test
    void rejectsApiKeyCreationWithoutAdminAuth() throws Exception {
        mvc.perform(post("/api/admin/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerEmail":"diego@example.com","tier":"FREE"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
