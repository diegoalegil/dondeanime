package com.dondeanime.backend.admin.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

@WebMvcTest(AdminTotpController.class)
@Import({
        SecurityConfig.class,
        AdminJwtService.class,
        AdminTotpManagementService.class
})
@TestPropertySource(properties = {
        "admin.username=admin",
        "admin.password=secret",
        "admin.cors.allowed-origins=http://localhost:4321",
        "alerts.jwt-secret=test-jwt-secret"
})
class AdminTotpControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AdminJwtService adminJwtService;

    @MockitoBean
    private AdminUserRepository adminUserRepository;

    @MockitoBean
    private AdminTotpService adminTotpService;

    @Test
    void statusRequiresAuth() throws Exception {
        mvc.perform(get("/api/admin/2fa"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void setupReturnsQrCodeData() throws Exception {
        stubAdminUser(null);
        when(adminTotpService.generateSecret()).thenReturn("SECRET");
        when(adminTotpService.buildOtpAuthUri("admin", "SECRET"))
                .thenReturn("otpauth://totp/DondeAnime%3Aadmin?secret=SECRET");

        mvc.perform(post("/api/admin/2fa/setup")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").value("SECRET"))
                .andExpect(jsonPath("$.qrCodeData").value("otpauth://totp/DondeAnime%3Aadmin?secret=SECRET"))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void verifyEnablesTotp() throws Exception {
        AdminUser adminUser = stubAdminUser(null);
        when(adminTotpService.isValidCode("SECRET", "123456")).thenReturn(true);

        mvc.perform(post("/api/admin/2fa/verify")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"secret":"SECRET","code":"123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        verify(adminUserRepository).save(adminUser);
    }

    @Test
    void verifyRejectsInvalidCode() throws Exception {
        stubAdminUser(null);
        when(adminTotpService.isValidCode("SECRET", "000000")).thenReturn(false);

        mvc.perform(post("/api/admin/2fa/verify")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"secret":"SECRET","code":"000000"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_totp"));
    }

    @Test
    void disableClearsTotpSecret() throws Exception {
        AdminUser adminUser = stubAdminUser("SECRET");

        mvc.perform(delete("/api/admin/2fa")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        verify(adminUserRepository).save(adminUser);
    }

    private AdminUser stubAdminUser(String secret) {
        AdminUser adminUser = new AdminUser();
        adminUser.setUsername("admin");
        adminUser.setTotpSecret(secret);
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(adminUserRepository.save(any(AdminUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return adminUser;
    }

    private String bearerToken() {
        return "Bearer " + adminJwtService.createAdminSession().token();
    }
}
