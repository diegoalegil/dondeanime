package com.dondeanime.backend.curated;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/admin/curators")
public class CuratorAdminController {

    private final CuratorProfileService service;

    public CuratorAdminController(CuratorProfileService service) {
        this.service = service;
    }

    @GetMapping
    public List<CuratorProfileDto> list() {
        return service.listCurators();
    }

    @PostMapping
    public CuratorProfileDto approve(@Valid @RequestBody CuratorProfileSaveRequest request) {
        return service.approveCurator(request);
    }

    @PostMapping("/{email}/revoke")
    public ResponseEntity<CuratorProfileDto> revoke(@PathVariable String email) {
        return service.revokeCurator(email)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
