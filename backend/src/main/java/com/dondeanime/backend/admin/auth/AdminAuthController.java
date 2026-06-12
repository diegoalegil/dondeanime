package com.dondeanime.backend.admin.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;

import com.dondeanime.backend.admin.auth.AdminAuthService.LoginResult;
import com.dondeanime.backend.admin.auth.AdminAuthService.LoginStatus;

@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AdminAuthService adminAuthService;
    private final AdminJwtService adminJwtService;
    private final AdminTokenRevocationService revocationService;

    public AdminAuthController(
            AdminAuthService adminAuthService,
            AdminJwtService adminJwtService,
            AdminTokenRevocationService revocationService) {
        this.adminAuthService = adminAuthService;
        this.adminJwtService = adminJwtService;
        this.revocationService = revocationService;
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

    /**
     * Revoca la sesión actual: el jti entra en la denylist hasta que el token
     * expire. El filtro ya validó el Bearer (la ruta exige authenticated),
     * aquí solo se extraen los claims para anotarlo. Idempotente.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            adminJwtService.validClaims(authorization.substring(BEARER_PREFIX.length()))
                    .ifPresent(revocationService::revoke);
        }
        return ResponseEntity.noContent().build();
    }
}
