package com.dondeanime.backend.admin.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

    @MockitoBean
    private AdminUserRepository adminUserRepository;

    @MockitoBean
    private AdminTotpService adminTotpService;

    @MockitoBean
    private AdminTokenRevocationService revocationService;

    @Test
    void loginReturnsBearerToken() throws Exception {
        stubAdminUser(null);

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
        stubAdminUser(null);

        mvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"bad"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRequiresTotpCodeWhenEnabled() throws Exception {
        stubAdminUser("SECRET");

        mvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"secret"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("totp_required"));
    }

    @Test
    void loginAcceptsValidTotpCodeWhenEnabled() throws Exception {
        stubAdminUser("SECRET");
        when(adminTotpService.isValidCode("SECRET", "123456")).thenReturn(true);

        mvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"secret","totpCode":"123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void adminEndpointsRejectBasicAuth() throws Exception {
        mvc.perform(get("/api/admin/anything")
                        .header("Authorization", "Basic YWRtaW46c2VjcmV0"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bearerTokenAuthenticatesAdminRequest() throws Exception {
        stubAdminUser(null);
        String token = adminJwtService.createAdminSession().token();

        mvc.perform(get("/api/admin/anything")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        assertThat(adminJwtService.isValidAdminToken(token)).isTrue();
    }

    @Test
    void logoutRevokesCurrentSession() throws Exception {
        String token = adminJwtService.createAdminSession().token();
        String jti = adminJwtService.validClaims(token).orElseThrow().jti();

        mvc.perform(post("/api/admin/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(revocationService)
                .revoke(org.mockito.ArgumentMatchers.argThat(claims -> jti.equals(claims.jti())));
    }

    @Test
    void logoutWithoutTokenIsRejectedByAuth() throws Exception {
        mvc.perform(post("/api/admin/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void revokedTokenNoLongerAuthenticates() throws Exception {
        String token = adminJwtService.createAdminSession().token();
        String jti = adminJwtService.validClaims(token).orElseThrow().jti();
        when(revocationService.isRevoked(jti)).thenReturn(true);

        mvc.perform(get("/api/admin/anything")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private void stubAdminUser(String totpSecret) {
        AdminUser adminUser = new AdminUser();
        adminUser.setUsername("admin");
        adminUser.setTotpSecret(totpSecret);
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(adminUserRepository.save(any(AdminUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
}
