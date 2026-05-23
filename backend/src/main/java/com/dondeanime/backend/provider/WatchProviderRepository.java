package com.dondeanime.backend.provider;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WatchProviderRepository extends JpaRepository<WatchProvider, Long> {

    /** Todos los providers de un anime, ordenados de forma estable para la UI. */
    List<WatchProvider> findByAnimeIdOrderByCountryCodeAscProviderTypeAscProviderNameAsc(Long animeId);

    /**
     * Borra todos los providers de un anime. Estrategia "delete + insert"
     * en cada sync: más simple que hacer upsert por composite key y para
     * el volumen actual (100 anime × ~5 providers) es trivial.
     *
     * Debe llamarse desde un método anotado con @Transactional.
     */
    void deleteByAnimeId(Long animeId);

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
}
