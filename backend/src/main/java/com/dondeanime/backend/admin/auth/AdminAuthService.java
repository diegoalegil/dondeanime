package com.dondeanime.backend.admin.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuthService {

    private final String username;
    private final String password;
    private final AdminJwtService adminJwtService;
    private final AdminUserRepository adminUserRepository;
    private final AdminTotpService adminTotpService;

    public AdminAuthService(
            @Value("${admin.username}") String username,
            @Value("${admin.password}") String password,
            AdminJwtService adminJwtService,
            AdminUserRepository adminUserRepository,
            AdminTotpService adminTotpService) {
        this.username = username;
        this.password = password;
        this.adminJwtService = adminJwtService;
        this.adminUserRepository = adminUserRepository;
        this.adminTotpService = adminTotpService;
    }

    @Transactional
    public LoginResult login(String username, String password, String totpCode) {
        if (!constantTimeEquals(this.username, username) || !constantTimeEquals(this.password, password)) {
            return LoginResult.invalid();
        }

        AdminUser adminUser = adminUser();
        if (adminUser.hasTotpEnabled() && !adminTotpService.isValidCode(adminUser.getTotpSecret(), totpCode)) {
            return LoginResult.requiresTotp();
        }

        return LoginResult.success(adminJwtService.createAdminSession());
    }

    private static boolean constantTimeEquals(String expected, String value) {
        if (expected == null || value == null) {
            return false;
        }

        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                value.getBytes(StandardCharsets.UTF_8));
    }

    private AdminUser adminUser() {
        return adminUserRepository.findByUsername(username)
                .orElseGet(() -> {
                    AdminUser created = new AdminUser();
                    created.setUsername(username);
                    return adminUserRepository.save(created);
                });
    }

    public record LoginResult(LoginStatus status, AdminLoginResponse session) {

        static LoginResult success(AdminLoginResponse session) {
            return new LoginResult(LoginStatus.SUCCESS, session);
        }

        static LoginResult requiresTotp() {
            return new LoginResult(LoginStatus.REQUIRES_TOTP, null);
        }

        static LoginResult invalid() {
            return new LoginResult(LoginStatus.INVALID, null);
        }
    }

    public enum LoginStatus {
        SUCCESS,
        REQUIRES_TOTP,
        INVALID
    }
}
