package com.dondeanime.backend.anime.tmdb;

import java.util.List;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Un resultado de búsqueda en TMDb.
 *
 * <p>Soporta tanto /search/tv como /search/multi: en series TMDb usa
 * {@code name}/{@code first_air_date}, en películas {@code title}/{@code release_date}.
 * Los accesores {@code displayName()}, {@code displayOriginalName()} y {@code displayDate()}
 * devuelven el valor correcto según el tipo. {@code media_type} viene poblado por /search/multi.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TmdbSearchResult(
        Long id,
        String name,
        String originalName,
        String overview,
        String firstAirDate,
        List<String> originCountry,
        String posterPath,
        Double popularity,
        String title,
        String originalTitle,
        String releaseDate,
        String mediaType,
        String originalLanguage
) {

    /** Título mostrado: serie usa {@code name}, película usa {@code title}. */
    public String displayName() {
        return name != null ? name : title;
    }

    /** Título original: serie usa {@code original_name}, película usa {@code original_title}. */
    public String displayOriginalName() {
        return originalName != null ? originalName : originalTitle;
    }

    /** Fecha de estreno: serie usa {@code first_air_date}, película usa {@code release_date}. */
    public String displayDate() {
        return firstAirDate != null ? firstAirDate : releaseDate;
    }

    /** Una respuesta de /search/tv no trae media_type; la tratamos como serie. */
    public boolean isTv() {
        return mediaType == null || "tv".equalsIgnoreCase(mediaType);
    }

    public boolean isMovie() {
        return "movie".equalsIgnoreCase(mediaType);
    }
}
