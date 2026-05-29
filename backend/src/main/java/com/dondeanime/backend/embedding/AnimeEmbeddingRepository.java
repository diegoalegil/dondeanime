package com.dondeanime.backend.embedding;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimeEmbeddingRepository extends JpaRepository<AnimeEmbedding, Long> {

    Optional<AnimeEmbedding> findByAnimeIdAndModel(Long animeId, String model);

    List<AnimeEmbedding> findByModelOrderByUpdatedAtDesc(String model);
}
