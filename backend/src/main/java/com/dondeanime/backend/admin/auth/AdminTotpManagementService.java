package com.dondeanime.backend.admin.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminTotpManagementService {

    private final String username;
    private final AdminUserRepository adminUserRepository;
    private final AdminTotpService adminTotpService;

    public AdminTotpManagementService(
            @Value("${admin.username}") String username,
            AdminUserRepository adminUserRepository,
            AdminTotpService adminTotpService) {
        this.username = username;
        this.adminUserRepository = adminUserRepository;
        this.adminTotpService = adminTotpService;
    }

    @Transactional(readOnly = true)
    public AdminTotpStatusResponse status() {
        return new AdminTotpStatusResponse(adminUserRepository.findByUsername(username)
                .map(AdminUser::hasTotpEnabled)
                .orElse(false));
    }

    @Transactional
    public AdminTotpSetupResponse setup() {
        AdminUser adminUser = adminUser();
        String secret = adminTotpService.generateSecret();
        String otpauthUri = adminTotpService.buildOtpAuthUri(adminUser.getUsername(), secret);
        return new AdminTotpSetupResponse(secret, otpauthUri, otpauthUri, adminUser.hasTotpEnabled());
    }

    @Transactional
    public AdminTotpStatusResponse verifyAndEnable(AdminTotpVerifyRequest request) {
        if (!adminTotpService.isValidCode(request.secret(), request.code())) {
            throw new InvalidTotpCodeException();
        }

        AdminUser adminUser = adminUser();
        adminUser.setTotpSecret(request.secret());
        adminUserRepository.save(adminUser);
        return new AdminTotpStatusResponse(true);
    }

    @Transactional
    public AdminTotpStatusResponse disable() {
        AdminUser adminUser = adminUser();
        adminUser.setTotpSecret(null);
        adminUserRepository.save(adminUser);
        return new AdminTotpStatusResponse(false);
    }

    private AdminUser adminUser() {
        return adminUserRepository.findByUsername(username)
                .orElseGet(() -> {
                    AdminUser created = new AdminUser();
                    created.setUsername(username);
                    return adminUserRepository.save(created);
                });
    }
}
