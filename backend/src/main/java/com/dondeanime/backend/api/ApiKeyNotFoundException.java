package com.dondeanime.backend.api;

public class ApiKeyNotFoundException extends RuntimeException {

    public ApiKeyNotFoundException() {
        super("API key invalida");
    }
}
