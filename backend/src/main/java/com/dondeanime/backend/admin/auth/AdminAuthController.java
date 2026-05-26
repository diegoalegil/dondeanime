package com.dondeanime.backend.admin.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;

import com.dondeanime.backend.admin.auth.AdminAuthService.LoginResult;
import com.dondeanime.backend.admin.auth.AdminAuthService.LoginStatus;

@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@Valid @RequestBody AdminLoginRequest request) {
        LoginResult result = adminAuthService.login(request.username(), request.password(), request.totpCode());
        if (result.status() == LoginStatus.SUCCESS) {
            return ResponseEntity.ok(result.session());
        }
        if (result.status() == LoginStatus.REQUIRES_TOTP) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new AdminLoginErrorResponse("totp_required"));
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
    }
}
