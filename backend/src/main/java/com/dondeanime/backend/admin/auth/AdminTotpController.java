package com.dondeanime.backend.admin.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/2fa")
public class AdminTotpController {

    private final AdminTotpManagementService adminTotpManagementService;

    public AdminTotpController(AdminTotpManagementService adminTotpManagementService) {
        this.adminTotpManagementService = adminTotpManagementService;
    }

    @GetMapping
    public AdminTotpStatusResponse status() {
        return adminTotpManagementService.status();
    }

    @PostMapping("/setup")
    public AdminTotpSetupResponse setup() {
        return adminTotpManagementService.setup();
    }

    @PostMapping("/verify")
    public AdminTotpStatusResponse verify(@Valid @RequestBody AdminTotpVerifyRequest request) {
        return adminTotpManagementService.verifyAndEnable(request);
    }

    @DeleteMapping
    public AdminTotpStatusResponse disable() {
        return adminTotpManagementService.disable();
    }

    @ExceptionHandler(InvalidTotpCodeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AdminLoginErrorResponse invalidTotp() {
        return new AdminLoginErrorResponse("invalid_totp");
    }
}
