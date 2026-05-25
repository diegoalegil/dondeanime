package com.dondeanime.backend.affiliate;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/admin/affiliate-links")
public class AffiliateLinkAdminController {

    private final AffiliateLinkService affiliateLinkService;

    public AffiliateLinkAdminController(AffiliateLinkService affiliateLinkService) {
        this.affiliateLinkService = affiliateLinkService;
    }

    @GetMapping
    public List<AffiliateLinkDto> list() {
        return affiliateLinkService.listLinks();
    }

    @PostMapping
    public AffiliateLinkDto save(@Valid @RequestBody AffiliateLinkRequest request) {
        return affiliateLinkService.saveLink(request);
    }

    @PostMapping("/bulk")
    public AffiliateBulkImportResult bulkImport(@RequestBody String csv) {
        return affiliateLinkService.bulkImport(csv);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        affiliateLinkService.deleteLink(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(AffiliateBulkImportException.class)
    public ResponseEntity<AffiliateBulkImportErrorResponse> bulkImportError(AffiliateBulkImportException exception) {
        return ResponseEntity.badRequest().body(new AffiliateBulkImportErrorResponse(exception.getErrors()));
    }
}
