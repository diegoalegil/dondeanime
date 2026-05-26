package com.dondeanime.backend.affiliate;

import java.util.List;

public record AffiliateBulkImportErrorResponse(List<AffiliateBulkImportError> errors) {
}
