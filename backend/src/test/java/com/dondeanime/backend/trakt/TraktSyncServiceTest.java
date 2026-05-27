package com.dondeanime.backend.trakt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;

class TraktSyncServiceTest {

    private final TraktHistoryClient traktHistoryClient = mock(TraktHistoryClient.class);
    private final ExternalAccountRepository accountRepository = mock(ExternalAccountRepository.class);
    private final ExternalAccountService externalAccountService = mock(ExternalAccountService.class);
    private final AnimeRepository animeRepository = mock(AnimeRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-27T12:00:00Z"), ZoneOffset.UTC);
    private final TraktSyncService service = new TraktSyncService(
            traktHistoryClient,
            accountRepository,
            externalAccountService,
            animeRepository,
            clock);

    @Test
    void importsWatchedHistoryAndRatingsForMatchedAnimeOnly() {
        ExternalAccount account = new ExternalAccount();
        account.setProvider("trakt");
        account.setExternalUserId("user-123");

        Anime attackOnTitan = anime("Attack on Titan", "Shingeki no Kyojin", 2013, "attack-on-titan");

        when(accountRepository.findByProviderAndExternalUserId("trakt", "user-123"))
                .thenReturn(Optional.of(account));
        when(animeRepository.findAll()).thenReturn(List.of(attackOnTitan));
        when(traktHistoryClient.fetchWatchedShows("access-token")).thenReturn(List.of(
                watched("Attack on Titan", 2013, "2026-05-20T19:00:00Z"),
                watched("Unknown Show", 2020, "2026-05-21T19:00:00Z")));
        when(traktHistoryClient.fetchRatedShows("access-token")).thenReturn(List.of(
                rated("Attack on Titan", 2013, 9, "2026-05-22T19:00:00Z")));

        TraktSyncResponse response = service.sync(new TraktSyncRequest(" user-123 ", " access-token "));

        assertThat(response.watchedImported()).isEqualTo(1);
        assertThat(response.ratingsImported()).isEqualTo(1);
        assertThat(response.unmatchedCount()).isEqualTo(1);
        assertThat(response.unmatched().getFirst().title()).isEqualTo("Unknown Show");
        verify(externalAccountService).markWatched(
                account,
                "attack-on-titan",
                Instant.parse("2026-05-20T19:00:00Z"),
                "TRAKT");
        verify(externalAccountService).markRating(
                account,
                "attack-on-titan",
                9,
                Instant.parse("2026-05-22T19:00:00Z"),
                "TRAKT");
        verify(accountRepository).save(account);
        assertThat(account.getLastSyncedAt()).isEqualTo("2026-05-27T12:00:00Z");
    }

    @Test
    void matchesRomajiTitleWhenEnglishTitleDoesNotMatch() {
        ExternalAccount account = new ExternalAccount();
        Anime anime = anime(null, "Shingeki no Kyojin", 2013, "attack-on-titan");

        when(accountRepository.findByProviderAndExternalUserId("trakt", "user-123"))
                .thenReturn(Optional.of(account));
        when(animeRepository.findAll()).thenReturn(List.of(anime));
        when(traktHistoryClient.fetchWatchedShows("access-token")).thenReturn(List.of(
                watched("Shingeki no Kyojin", 2013, "2026-05-20T19:00:00Z")));
        when(traktHistoryClient.fetchRatedShows("access-token")).thenReturn(List.of());

        TraktSyncResponse response = service.sync(new TraktSyncRequest("user-123", "access-token"));

        assertThat(response.watchedImported()).isEqualTo(1);
        assertThat(response.unmatchedCount()).isZero();
    }

    private static Anime anime(String titleEnglish, String titleRomaji, Integer year, String slug) {
        Anime anime = new Anime();
        anime.setTitleEnglish(titleEnglish);
        anime.setTitleRomaji(titleRomaji);
        anime.setStartYear(year);
        anime.setSlug(slug);
        return anime;
    }

    private static TraktWatchedShow watched(String title, Integer year, String lastWatchedAt) {
        return new TraktWatchedShow(
                1,
                Instant.parse(lastWatchedAt),
                new TraktShow(title, year, new TraktShowIds(1L, "slug", null, null)));
    }

    private static TraktRatedShow rated(String title, Integer year, Integer rating, String ratedAt) {
        return new TraktRatedShow(
                rating,
                Instant.parse(ratedAt),
                new TraktShow(title, year, new TraktShowIds(1L, "slug", null, null)));
    }
}
