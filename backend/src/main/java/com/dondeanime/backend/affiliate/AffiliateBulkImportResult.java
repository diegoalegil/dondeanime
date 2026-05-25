package com.dondeanime.backend.affiliate;

import java.util.List;

public record AffiliateBulkImportResult(
        int imported,
        List<AffiliateLinkDto> links) {
}
