package com.dondeanime.backend.provider;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface WatchProviderRepository extends JpaRepository<WatchProvider, Long> {

    /** Todos los providers de un anime, ordenados de forma estable para la UI. */
    List<WatchProvider> findByAnimeIdOrderByCountryCodeAscProviderTypeAscProviderNameAsc(Long animeId);

    /**
     * Instante de la última escritura de providers (watchdog de frescura del
     * "dónde verlo"). El sync de providers tiene su propio cron, distinto del de
     * AniList; sin este dato el watchdog solo vigilaba {@code Anime.syncedAt} y no
     * detectaba un job de providers caído. Vacío si aún no hay providers.
     */
    @Query("SELECT MAX(wp.updatedAt) FROM WatchProvider wp")
    Optional<Instant> findMaxUpdatedAt();

    @Query("""
            SELECT wp FROM WatchProvider wp
            WHERE wp.animeId IN :animeIds
            ORDER BY wp.animeId ASC, wp.countryCode ASC, wp.providerType ASC, wp.providerName ASC
            """)
    List<WatchProvider> findByAnimeIdInOrderByAnimeIdAscCountryCodeAscProviderTypeAscProviderNameAsc(
            @Param("animeIds") List<Long> animeIds);

    /**
     * Borra todos los providers de un anime e inmediatamente los persiste.
     *
     * Usa @Modifying + JPQL DELETE explícito porque la versión derived query
     * (Spring Data autogenerada) no garantizaba que el DELETE se ejecutara
     * antes de los INSERTs subsecuentes dentro de la misma transacción:
     * Hibernate diferia el DELETE hasta el commit, los INSERTs salían
     * primero y chocaban con la unique constraint.
     *
     * Con @Modifying + flushAutomatically=true el DELETE se ejecuta YA y
     * los INSERTs después no encuentran duplicados.
     *
     * Debe llamarse desde un método con transacción activa.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM WatchProvider w WHERE w.animeId = :animeId")
    void deleteByAnimeId(Long animeId);

    @Query("""
            SELECT a.slug AS animeSlug,
                   LOWER(REPLACE(wp.providerName, ' ', '-')) AS providerSlug
            FROM WatchProvider wp
            JOIN Anime a ON a.id = wp.animeId
            WHERE a.slug IN :animeSlugs
            """)
    List<AnimeProviderProjection> findProviderSlugsByAnimeSlugs(@Param("animeSlugs") Set<String> animeSlugs);

    interface AnimeProviderProjection {
        String getAnimeSlug();
        String getProviderSlug();
    }

    /**
     * Agrega providers a nivel global: por cada providerName devuelve
     * un logo (cualquiera) y el número de animes distintos.
     * Ordenado por count descendente para que las plataformas más usadas
     * aparezcan arriba en la página de "todas las plataformas".
     */
    @Query("""
            SELECT wp.providerName AS providerName,
                   MAX(wp.logoUrl) AS logoUrl,
                   COUNT(DISTINCT wp.animeId) AS animeCount
            FROM WatchProvider wp
            GROUP BY wp.providerName
            ORDER BY COUNT(DISTINCT wp.animeId) DESC, wp.providerName ASC
            """)
    List<ProviderAggregation> aggregateAllProviders();

    /**
     * Misma agregación pero filtrada por país.
     */
    @Query("""
            SELECT wp.providerName AS providerName,
                   MAX(wp.logoUrl) AS logoUrl,
                   COUNT(DISTINCT wp.animeId) AS animeCount
            FROM WatchProvider wp
            WHERE wp.countryCode = :countryCode
            GROUP BY wp.providerName
            ORDER BY COUNT(DISTINCT wp.animeId) DESC, wp.providerName ASC
            """)
    List<ProviderAggregation> aggregateProvidersByCountry(String countryCode);

    /**
     * Projection interface para las queries de agregación. Spring Data
     * implementa el getter por nombre del alias en la query.
     */
    interface ProviderAggregation {
        String getProviderName();
        String getLogoUrl();
        Long getAnimeCount();
    }

    /**
     * Pares únicos (providerName, countryCode) para el sitemap.
     */
    @Query("""
            SELECT DISTINCT wp.providerName AS providerName,
                            wp.countryCode AS countryCode
            FROM WatchProvider wp
            ORDER BY wp.providerName, wp.countryCode
            """)
    List<ProviderCountryAggregation> aggregateProviderCountries();

    interface ProviderCountryAggregation {
        String getProviderName();
        String getCountryCode();
    }
}
