package com.dondeanime.backend.trakt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;

class TraktSyncServiceTest {

    private final TraktClient traktClient = mock(TraktClient.class);
    private final TraktHistoryClient traktHistoryClient = mock(TraktHistoryClient.class);
    private final ExternalAccountRepository accountRepository = mock(ExternalAccountRepository.class);
    private final ExternalAccountService externalAccountService = mock(ExternalAccountService.class);
    private final TraktSyncEventRepository syncEventRepository = mock(TraktSyncEventRepository.class);
    private final AnimeRepository animeRepository = mock(AnimeRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-27T12:00:00Z"), ZoneOffset.UTC);
    private final TraktTokenCipherService tokenCipherService =
            new TraktTokenCipherService("test-secret-with-enough-entropy");
    private final TraktSyncService service = new TraktSyncService(
            traktClient,
            traktHistoryClient,
            accountRepository,
            externalAccountService,
            syncEventRepository,
            animeRepository,
            tokenCipherService,
            clock,
            true);

    @Test
    void importsWatchedHistoryAndRatingsForMatchedAnimeOnly() {
        ExternalAccount account = account("user-123", "access-token", "refresh-token");

        Anime attackOnTitan = anime("Attack on Titan", "Shingeki no Kyojin", 2013, "attack-on-titan");

        when(accountRepository.findByProviderAndExternalUserId("trakt", "user-123"))
                .thenReturn(Optional.of(account));
        when(animeRepository.findAll()).thenReturn(List.of(attackOnTitan));
        when(traktHistoryClient.fetchWatchedShows("access-token")).thenReturn(List.of(
                watched("Attack on Titan", 2013, "2026-05-20T19:00:00Z"),
                watched("Unknown Show", 2020, "2026-05-21T19:00:00Z")));
        when(traktHistoryClient.fetchRatedShows("access-token")).thenReturn(List.of(
                rated("Attack on Titan", 2013, 9, "2026-05-22T19:00:00Z")));

        TraktSyncResponse response = service.sync(" user-123 ");

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
        ArgumentCaptor<TraktSyncEvent> eventCaptor = ArgumentCaptor.forClass(TraktSyncEvent.class);
        verify(syncEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getProvider()).isEqualTo("trakt");
        assertThat(eventCaptor.getValue().getWatchedImported()).isEqualTo(1);
        assertThat(eventCaptor.getValue().getRatingsImported()).isEqualTo(1);
        assertThat(eventCaptor.getValue().getUnmatchedCount()).isEqualTo(1);
        assertThat(eventCaptor.getValue().getSyncedAt()).isEqualTo("2026-05-27T12:00:00Z");
        assertThat(account.getLastSyncedAt()).isEqualTo("2026-05-27T12:00:00Z");
    }

    @Test
    void matchesRomajiTitleWhenEnglishTitleDoesNotMatch() {
        ExternalAccount account = account("user-123", "access-token", "refresh-token");
        Anime anime = anime(null, "Shingeki no Kyojin", 2013, "attack-on-titan");

        when(accountRepository.findByProviderAndExternalUserId("trakt", "user-123"))
                .thenReturn(Optional.of(account));
        when(animeRepository.findAll()).thenReturn(List.of(anime));
        when(traktHistoryClient.fetchWatchedShows("access-token")).thenReturn(List.of(
                watched("Shingeki no Kyojin", 2013, "2026-05-20T19:00:00Z")));
        when(traktHistoryClient.fetchRatedShows("access-token")).thenReturn(List.of());

        TraktSyncResponse response = service.sync("user-123");

        assertThat(response.watchedImported()).isEqualTo(1);
        assertThat(response.unmatchedCount()).isZero();
    }

    @Test
    void refreshesExpiredTokenBeforeSyncing() {
        ExternalAccount account = account("user-123", "old-access-token", "refresh-token");
        account.setTokenExpiresAt(Instant.parse("2026-05-27T11:59:30Z"));
        Anime anime = anime("Attack on Titan", "Shingeki no Kyojin", 2013, "attack-on-titan");

        when(accountRepository.findByProviderAndExternalUserId("trakt", "user-123"))
                .thenReturn(Optional.of(account));
        when(traktClient.refreshAccessToken("refresh-token"))
                .thenReturn(new TraktOAuthTokenResponse(
                        "new-access-token",
                        "new-refresh-token",
                        "bearer",
                        7200L,
                        "public watched",
                        1780000000L));
        when(animeRepository.findAll()).thenReturn(List.of(anime));
        when(traktHistoryClient.fetchWatchedShows("new-access-token")).thenReturn(List.of());
        when(traktHistoryClient.fetchRatedShows("new-access-token")).thenReturn(List.of());

        service.sync("user-123");

        ArgumentCaptor<ExternalAccountUpsertCommand> commandCaptor =
                ArgumentCaptor.forClass(ExternalAccountUpsertCommand.class);
        verify(externalAccountService).upsert(commandCaptor.capture());
        ExternalAccountUpsertCommand command = commandCaptor.getValue();
        assertThat(tokenCipherService.decrypt(command.accessTokenCiphertext())).isEqualTo("new-access-token");
        assertThat(tokenCipherService.decrypt(command.refreshTokenCiphertext())).isEqualTo("new-refresh-token");
        assertThat(command.scopes()).isEqualTo("public watched");
        assertThat(command.tokenExpiresAt()).isEqualTo("2026-05-28T22:26:40Z");
    }

    @Test
    void reloadsAccountBeforeSavingToAvoidClobberingRefreshedTokens() {
        // Token caducado: fuerza el refresh, que persiste los tokens nuevos vía
        // upsert (su propia transacción). El objeto cargado al inicio queda
        // stale; si el save final lo usara, pisaría el token recién refrescado.
        ExternalAccount stale = account("user-123", "old-access-token", "refresh-token");
        stale.setTokenExpiresAt(Instant.parse("2026-05-27T11:59:30Z"));
        ExternalAccount fresh = account("user-123", "new-access-token", "new-refresh-token");

        when(accountRepository.findByProviderAndExternalUserId("trakt", "user-123"))
                .thenReturn(Optional.of(stale))
                .thenReturn(Optional.of(fresh));
        when(traktClient.refreshAccessToken("refresh-token"))
                .thenReturn(new TraktOAuthTokenResponse(
                        "new-access-token",
                        "new-refresh-token",
                        "bearer",
                        7200L,
                        "public watched",
                        1780000000L));
        when(animeRepository.findAll()).thenReturn(List.of());
        when(traktHistoryClient.fetchWatchedShows("new-access-token")).thenReturn(List.of());
        when(traktHistoryClient.fetchRatedShows("new-access-token")).thenReturn(List.of());

        service.sync("user-123");

        // El save final usa la cuenta RECARGADA; la stale queda intacta.
        verify(accountRepository).save(fresh);
        assertThat(fresh.getLastSyncedAt()).isEqualTo("2026-05-27T12:00:00Z");
        assertThat(stale.getLastSyncedAt()).isNull();
    }

    @Test
    void deduplicatesRepeatedShowsFromTrakt() {
        ExternalAccount account = account("user-123", "access-token", "refresh-token");
        Anime attackOnTitan = anime("Attack on Titan", "Shingeki no Kyojin", 2013, "attack-on-titan");

        when(accountRepository.findByProviderAndExternalUserId("trakt", "user-123"))
                .thenReturn(Optional.of(account));
        when(animeRepository.findAll()).thenReturn(List.of(attackOnTitan));
        when(traktHistoryClient.fetchWatchedShows("access-token")).thenReturn(List.of(
                watched("Attack on Titan", 2013, "2026-05-20T19:00:00Z"),
                watched("Attack on Titan", 2013, "2026-05-21T19:00:00Z")));
        when(traktHistoryClient.fetchRatedShows("access-token")).thenReturn(List.of(
                rated("Attack on Titan", 2013, 9, "2026-05-22T19:00:00Z"),
                rated("Attack on Titan", 2013, 10, "2026-05-23T19:00:00Z")));

        TraktSyncResponse response = service.sync("user-123");

        assertThat(response.watchedImported()).isEqualTo(1);
        assertThat(response.ratingsImported()).isEqualTo(1);
        verify(externalAccountService, times(1)).markWatched(
                account,
                "attack-on-titan",
                Instant.parse("2026-05-20T19:00:00Z"),
                "TRAKT");
        verify(externalAccountService, times(1)).markRating(
                account,
                "attack-on-titan",
                9,
                Instant.parse("2026-05-22T19:00:00Z"),
                "TRAKT");
    }

    @Test
    void externalFetchErrorReturnsBadGatewayWithoutSavingSyncEvent() {
        ExternalAccount account = account("user-123", "access-token", "refresh-token");

        when(accountRepository.findByProviderAndExternalUserId("trakt", "user-123"))
                .thenReturn(Optional.of(account));
        when(animeRepository.findAll()).thenReturn(List.of());
        when(traktHistoryClient.fetchWatchedShows("access-token"))
                .thenThrow(new RestClientException("trakt down"));

        assertThatThrownBy(() -> service.sync("user-123"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
                .isEqualTo(502);

        verify(syncEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ratingsFetchErrorDoesNotPersistWatchedPartialResults() {
        ExternalAccount account = account("user-123", "access-token", "refresh-token");
        Anime attackOnTitan = anime("Attack on Titan", "Shingeki no Kyojin", 2013, "attack-on-titan");

        when(accountRepository.findByProviderAndExternalUserId("trakt", "user-123"))
                .thenReturn(Optional.of(account));
        when(animeRepository.findAll()).thenReturn(List.of(attackOnTitan));
        when(traktHistoryClient.fetchWatchedShows("access-token")).thenReturn(List.of(
                watched("Attack on Titan", 2013, "2026-05-20T19:00:00Z")));
        when(traktHistoryClient.fetchRatedShows("access-token"))
                .thenThrow(new RestClientException("trakt down"));

        assertThatThrownBy(() -> service.sync("user-123"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
                .isEqualTo(502);

        verify(externalAccountService, never()).markWatched(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        verify(syncEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void missingStoredTokensRejectsSync() {
        ExternalAccount account = new ExternalAccount();
        account.setProvider("trakt");
        account.setExternalUserId("user-123");

        when(accountRepository.findByProviderAndExternalUserId("trakt", "user-123"))
                .thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.sync("user-123"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
                .isEqualTo(409);
    }

    @Test
    void disabledFeatureRejectsSyncBeforeLookingUpAccount() {
        TraktSyncService disabled = new TraktSyncService(
                traktClient,
                traktHistoryClient,
                accountRepository,
                externalAccountService,
                syncEventRepository,
                animeRepository,
                tokenCipherService,
                clock,
                false);

        assertThatThrownBy(() -> disabled.sync("user-123"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
                .isEqualTo(503);

        verify(accountRepository, never()).findByProviderAndExternalUserId("trakt", "user-123");
    }

    private ExternalAccount account(String externalUserId, String accessToken, String refreshToken) {
        ExternalAccount account = new ExternalAccount();
        account.setProvider("trakt");
        account.setExternalUserId(externalUserId);
        account.setEmail("diego@example.com");
        account.setAccessTokenCiphertext(tokenCipherService.encrypt(accessToken));
        account.setRefreshTokenCiphertext(tokenCipherService.encrypt(refreshToken));
        account.setScopes("public watched");
        account.setTokenExpiresAt(Instant.parse("2026-05-27T13:00:00Z"));
        return account;
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
