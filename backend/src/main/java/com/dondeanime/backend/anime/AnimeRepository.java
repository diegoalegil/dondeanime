package com.dondeanime.backend.anime;

import java.util.List;
import java.util.Optional;

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

    /**
     * Lista de slugs activos (para sitemap).
     */
    @Query("SELECT a.slug FROM Anime a WHERE a.slug IS NOT NULL ORDER BY a.slug")
    List<String> findAllSlugs();
}
