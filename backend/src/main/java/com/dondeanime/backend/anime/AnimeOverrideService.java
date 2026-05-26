package com.dondeanime.backend.anime;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AnimeOverrideService {

    public static final String DEFAULT_LOCALE = "es";

    private static final Set<String> OVERRIDEABLE_FIELDS = Set.of(
            "description",
            "title_english",
            "title_romaji",
            "beginner_recommendation");

    private final AnimeOverrideRepository repository;

    public AnimeOverrideService(AnimeOverrideRepository repository) {
        this.repository = repository;
    }

    public List<AnimeOverride> findSpanishOverrides(Anime anime) {
        return repository.findByAnime_IdAndLocaleOrderByFieldNameAsc(
                anime.getId(),
                DEFAULT_LOCALE);
    }

    public List<AnimeOverride> listOverrides(Anime anime) {
        return repository.findByAnime_IdOrderByLocaleAscFieldNameAsc(anime.getId());
    }

    public AnimeOverride saveOverride(
            Anime anime,
            String rawFieldName,
            String rawFieldValue,
            String rawLocale,
            String updatedBy) {
        String fieldName = normalizeFieldName(rawFieldName);
        String locale = normalizeLocale(rawLocale);
        String fieldValue = normalizeFieldValue(rawFieldValue);

        AnimeOverride override = repository
                .findByAnime_IdAndFieldNameAndLocale(anime.getId(), fieldName, locale)
                .orElseGet(AnimeOverride::new);

        override.setAnime(anime);
        override.setFieldName(fieldName);
        override.setFieldValue(fieldValue);
        override.setLocale(locale);
        override.setUpdatedAt(Instant.now());
        override.setUpdatedBy(updatedBy);

        return repository.save(override);
    }

    public void deleteOverride(Anime anime, String rawFieldName, String rawLocale) {
        String fieldName = normalizeFieldName(rawFieldName);
        String locale = normalizeLocale(rawLocale);

        repository.findByAnime_IdAndFieldNameAndLocale(anime.getId(), fieldName, locale)
                .ifPresent(repository::delete);
    }

    public String originalValue(Anime anime, String rawFieldName) {
        String fieldName = normalizeFieldName(rawFieldName);

        return switch (fieldName) {
            case "description" -> anime.getDescription();
            case "title_english" -> anime.getTitleEnglish();
            case "title_romaji" -> anime.getTitleRomaji();
            case "beginner_recommendation" -> null;
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Campo no editable: " + rawFieldName);
        };
    }

    public static String normalizeFieldName(String rawFieldName) {
        if (rawFieldName == null || rawFieldName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fieldName es obligatorio");
        }

        String fieldName = rawFieldName.trim()
                .replace("titleEnglish", "title_english")
                .replace("titleRomaji", "title_romaji")
                .replace("beginnerRecommendation", "beginner_recommendation")
                .toLowerCase(Locale.ROOT);

        if (!OVERRIDEABLE_FIELDS.contains(fieldName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campo no editable: " + rawFieldName);
        }

        return fieldName;
    }

    public static String normalizeLocale(String rawLocale) {
        if (rawLocale == null || rawLocale.isBlank()) {
            return DEFAULT_LOCALE;
        }

        String locale = rawLocale.trim().toLowerCase(Locale.ROOT);
        if (locale.length() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "locale debe tener 5 caracteres o menos");
        }

        return locale;
    }

    private static String normalizeFieldValue(String rawFieldValue) {
        if (rawFieldValue == null || rawFieldValue.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fieldValue es obligatorio");
        }

        return rawFieldValue.trim();
    }
}
