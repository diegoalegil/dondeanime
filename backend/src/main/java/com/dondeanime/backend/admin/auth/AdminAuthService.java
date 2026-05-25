package com.dondeanime.backend.admin.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {

    private final String username;
    private final String password;
    private final AdminJwtService adminJwtService;

    public AdminAuthService(
            @Value("${admin.username}") String username,
            @Value("${admin.password}") String password,
            AdminJwtService adminJwtService) {
        this.username = username;
        this.password = password;
        this.adminJwtService = adminJwtService;
    }

    public Optional<AdminLoginResponse> login(String username, String password) {
        if (!constantTimeEquals(this.username, username) || !constantTimeEquals(this.password, password)) {
            return Optional.empty();
        }

        return Optional.of(adminJwtService.createAdminSession());
    }

    private static boolean constantTimeEquals(String expected, String value) {
        if (expected == null || value == null) {
            return false;
        }

        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                value.getBytes(StandardCharsets.UTF_8));
    }
}
