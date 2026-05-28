package com.dondeanime.backend.character;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimeCharacterRepository extends JpaRepository<AnimeCharacter, Long> {

    Optional<AnimeCharacter> findByAnilistId(Long anilistId);
}
