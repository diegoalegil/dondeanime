package com.dondeanime.backend.anime;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AnimeRepository extends JpaRepository<Anime, Long> {

    Optional<Anime> findByAnilistId(Long anilistId);

    Optional<Anime> findBySlug(String slug);

    /**
     * Anime disponibles en una plataforma concreta en un país concreto.
     * El slug del provider sigue la convención de ProviderSummaryDto:
     * lowercase + espacios→guiones ("Amazon Prime Video" → "amazon-prime-video").
     *
     * Ordenados por popularidad descendente (los más populares primero,
     * útil para listings SEO).
     */
    @Query("""
            SELECT DISTINCT a FROM Anime a
            WHERE a.id IN (
                SELECT wp.animeId FROM WatchProvider wp
                WHERE LOWER(REPLACE(wp.providerName, ' ', '-')) = :providerSlug
                AND wp.countryCode = :countryCode
            )
            ORDER BY a.popularity DESC NULLS LAST, a.titleEnglish ASC
            """)
    List<Anime> findByProviderSlugAndCountry(String providerSlug, String countryCode);

    /**
     * Anime que tienen un género concreto.
     * El género se compara case-insensitive con el slug recibido:
     * "Slice of Life" matchea "slice-of-life".
     */
    @Query("""
            SELECT DISTINCT a FROM Anime a
            JOIN a.genres g
            WHERE LOWER(REPLACE(g, ' ', '-')) = :genreSlug
            ORDER BY a.popularity DESC NULLS LAST, a.titleEnglish ASC
            """)
    List<Anime> findByGenreSlug(String genreSlug);

    /**
     * Anime de una temporada concreta (ej. WINTER 2024).
     */
    @Query("""
            SELECT a FROM Anime a
            WHERE a.seasonYear = :year AND a.season = :season
            ORDER BY a.popularity DESC NULLS LAST, a.titleEnglish ASC
            """)
    List<Anime> findBySeasonYearAndSeason(int year, String season);

    @Query("""
            SELECT DISTINCT a FROM Anime a
            JOIN a.genres g
            WHERE a.id <> :animeId
            AND g = :primaryGenre
            AND a.averageScore > :minScore
            ORDER BY a.averageScore DESC NULLS LAST, a.popularity DESC NULLS LAST, a.titleEnglish ASC
            """)
    List<Anime> findSimilarByPrimaryGenre(Long animeId, String primaryGenre, int minScore, Pageable pageable);

    @Query("""
            SELECT a FROM Anime a
            WHERE a.id <> :animeId
            AND LOWER(a.primaryStudio) = LOWER(:primaryStudio)
            AND a.averageScore > :minScore
            ORDER BY a.averageScore DESC NULLS LAST, a.popularity DESC NULLS LAST, a.titleEnglish ASC
            """)
    List<Anime> findSimilarByPrimaryStudio(Long animeId, String primaryStudio, int minScore, Pageable pageable);

    /**
     * Anime con match TMDb pero sin descripción localizada en español.
     */
    @Query("""
            SELECT a FROM Anime a
            WHERE a.tmdbId IS NOT NULL
            AND (a.descriptionEs IS NULL OR TRIM(a.descriptionEs) = '')
            ORDER BY a.popularity DESC NULLS LAST, a.titleEnglish ASC
            """)
    List<Anime> findWithTmdbIdAndMissingDescriptionEs();

    /**
     * Lista de slugs activos (para sitemap).
     */
    @Query("SELECT a.slug FROM Anime a WHERE a.slug IS NOT NULL ORDER BY a.slug")
    List<String> findAllSlugs();

    /**
     * Géneros agregados con count de anime distintos.
     * Ordenado por count desc para que los más usados aparezcan arriba.
     */
    @Query("""
            SELECT g AS genre, COUNT(DISTINCT a.id) AS animeCount
            FROM Anime a JOIN a.genres g
            GROUP BY g
            ORDER BY COUNT(DISTINCT a.id) DESC, g ASC
            """)
    List<GenreAggregation> aggregateGenres();

    /**
     * Temporadas agregadas (año + season) con count.
     * Ordenadas de más reciente a más antigua.
     */
    @Query("""
            SELECT a.seasonYear AS year, a.season AS season, COUNT(a) AS animeCount
            FROM Anime a
            WHERE a.seasonYear IS NOT NULL AND a.season IS NOT NULL
            GROUP BY a.seasonYear, a.season
            ORDER BY a.seasonYear DESC, a.season ASC
            """)
    List<SeasonAggregation> aggregateSeasons();

    interface GenreAggregation {
        String getGenre();
        Long getAnimeCount();
    }

    interface SeasonAggregation {
        Integer getYear();
        String getSeason();
        Long getAnimeCount();
    }
}
