package com.dondeanime.backend.subscription;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class CountryCatalog {

    private static final Map<String, String> COUNTRY_NAMES = Map.of(
            "ES", "España",
            "MX", "México",
            "AR", "Argentina",
            "CO", "Colombia",
            "CL", "Chile");

    private CountryCatalog() {
    }

    public static String normalizeCountry(String rawCountry) {
        if (rawCountry == null || rawCountry.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "country es obligatorio");
        }

        String country = rawCountry.trim().toUpperCase(Locale.ROOT);
        if (!COUNTRY_NAMES.containsKey(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "País no soportado: " + rawCountry);
        }

        return country;
    }

    public static String countryName(String countryCode) {
        return COUNTRY_NAMES.getOrDefault(countryCode, countryCode);
    }

    public static Set<String> supportedCountries() {
        return COUNTRY_NAMES.keySet();
    }
}
