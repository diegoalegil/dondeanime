package com.dondeanime.backend.api;

public class ApiQuotaExceededException extends RuntimeException {

    public ApiQuotaExceededException() {
        super("Cuota mensual agotada");
    }
}
