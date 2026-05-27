package com.dondeanime.backend.trakt;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserWatchedAnimeRepository extends JpaRepository<UserWatchedAnime, Long> {

    Optional<UserWatchedAnime> findByExternalAccountAndAnimeSlugAndSource(
            ExternalAccount externalAccount,
            String animeSlug,
            String source);

    List<UserWatchedAnime> findByExternalAccountOrderByWatchedAtDesc(ExternalAccount externalAccount);
}
