package com.dondeanime.backend.affiliate;

import java.util.List;

public class AffiliateBulkImportException extends RuntimeException {

    private final List<AffiliateBulkImportError> errors;

    public AffiliateBulkImportException(List<AffiliateBulkImportError> errors) {
        this.errors = errors;
    }

    public List<AffiliateBulkImportError> getErrors() {
        return errors;
    }
}
