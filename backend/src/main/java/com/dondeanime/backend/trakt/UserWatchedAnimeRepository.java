package com.dondeanime.backend.trakt;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserWatchedAnimeRepository extends JpaRepository<UserWatchedAnime, Long> {

    Optional<UserWatchedAnime> findByExternalAccountAndAnimeSlugAndSource(
            ExternalAccount externalAccount,
            String animeSlug,
            String source);

    List<UserWatchedAnime> findByExternalAccountOrderByWatchedAtDesc(ExternalAccount externalAccount);

    @Query("""
            SELECT DISTINCT w.animeSlug
            FROM UserWatchedAnime w
            WHERE w.externalAccount.provider = :provider
            AND w.externalAccount.externalUserId = :externalUserId
            ORDER BY w.animeSlug ASC
            """)
    List<String> findWatchedSlugs(
            @Param("provider") String provider,
            @Param("externalUserId") String externalUserId);
}
