package com.dondeanime.backend.anime;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimeOverrideRepository extends JpaRepository<AnimeOverride, Long> {

    List<AnimeOverride> findByAnime_IdAndLocaleOrderByFieldNameAsc(Long animeId, String locale);

    List<AnimeOverride> findByAnime_IdOrderByLocaleAscFieldNameAsc(Long animeId);

    Optional<AnimeOverride> findByAnime_IdAndFieldNameAndLocale(
            Long animeId,
            String fieldName,
            String locale);
}
