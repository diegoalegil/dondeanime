package com.dondeanime.backend.provider;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
