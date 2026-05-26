package com.dondeanime.backend.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/api-keys")
public class ApiKeyAdminController {

    private final ApiKeyService apiKeyService;

    public ApiKeyAdminController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiKeyDto create(@Valid @RequestBody ApiKeyCreateRequest request) {
        return apiKeyService.create(request);
    }

    @GetMapping("/stats")
    public ApiKeyStatsDto stats() {
        return apiKeyService.stats();
    }
}
