package com.dondeanime.backend.anime;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimeRepository extends JpaRepository<Anime, Long> {

    Optional<Anime> findByAnilistId(Long anilistId);

    Optional<Anime> findBySlug(String slug);
}
