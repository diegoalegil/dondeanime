package com.dondeanime.backend.admin.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.dondeanime.backend.config.SecurityConfig;

@WebMvcTest(AdminAuthController.class)
@Import({
        SecurityConfig.class,
        AdminAuthService.class,
        AdminJwtService.class
})
@TestPropertySource(properties = {
        "admin.username=admin",
        "admin.password=secret",
        "admin.cors.allowed-origins=http://localhost:4321",
        "alerts.jwt-secret=test-jwt-secret"
})
class AdminAuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AdminJwtService adminJwtService;

    @Test
    void loginReturnsBearerToken() throws Exception {
        mvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"secret"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.expiresAt").isString());
    }

    @Test
    void loginRejectsBadPassword() throws Exception {
        mvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"bad"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpointsRejectBasicAuth() throws Exception {
        mvc.perform(get("/api/admin/anything")
                        .header("Authorization", "Basic YWRtaW46c2VjcmV0"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bearerTokenAuthenticatesAdminRequest() throws Exception {
        String token = adminJwtService.createAdminSession().token();

        mvc.perform(get("/api/admin/anything")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        assertThat(adminJwtService.isValidAdminToken(token)).isTrue();
    }
}
