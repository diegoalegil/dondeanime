package com.dondeanime.backend.trakt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class ExternalAccountServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-27T11:00:00Z");

    private final ExternalAccountRepository accountRepository = mock(ExternalAccountRepository.class);
    private final UserWatchedAnimeRepository watchedAnimeRepository = mock(UserWatchedAnimeRepository.class);
    private final ExternalAccountService service = new ExternalAccountService(
            accountRepository,
            watchedAnimeRepository,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void upsertNormalizesAccountFieldsAndScopes() {
        when(accountRepository.findByProviderAndExternalUserId("trakt", "user-123"))
                .thenReturn(Optional.empty());
        when(accountRepository.save(any(ExternalAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ExternalAccount account = service.upsert(new ExternalAccountUpsertCommand(
                " Trakt ",
                " user-123 ",
                " Diego@Example.COM ",
                " access-cipher ",
                " refresh-cipher ",
                "watched public,watched",
                Instant.parse("2026-05-27T13:00:00Z")));

        assertThat(account.getProvider()).isEqualTo("trakt");
        assertThat(account.getExternalUserId()).isEqualTo("user-123");
        assertThat(account.getEmail()).isEqualTo("diego@example.com");
        assertThat(account.getAccessTokenCiphertext()).isEqualTo("access-cipher");
        assertThat(account.getRefreshTokenCiphertext()).isEqualTo("refresh-cipher");
        assertThat(account.getScopes()).isEqualTo("public watched");
        assertThat(account.getCreatedAt()).isEqualTo(NOW);
        assertThat(account.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void upsertUpdatesExistingAccountWithoutChangingCreatedAt() {
        ExternalAccount existing = new ExternalAccount();
        existing.setProvider("trakt");
        existing.setExternalUserId("user-123");
        existing.setCreatedAt(Instant.parse("2026-05-26T11:00:00Z"));

        when(accountRepository.findByProviderAndExternalUserId("trakt", "user-123"))
                .thenReturn(Optional.of(existing));
        when(accountRepository.save(any(ExternalAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ExternalAccount account = service.upsert(new ExternalAccountUpsertCommand(
                "trakt",
                "user-123",
                null,
                null,
                "refresh-cipher",
                "public",
                null));

        assertThat(account.getCreatedAt()).isEqualTo("2026-05-26T11:00:00Z");
        assertThat(account.getUpdatedAt()).isEqualTo(NOW);
        assertThat(account.getRefreshTokenCiphertext()).isEqualTo("refresh-cipher");
    }

    @Test
    void markWatchedNormalizesAndDeduplicatesAnime() {
        ExternalAccount account = new ExternalAccount();
        account.setProvider("trakt");
        account.setExternalUserId("user-123");

        when(watchedAnimeRepository.findByExternalAccountAndAnimeSlugAndSource(
                account,
                "attack-on-titan",
                "TRAKT"))
                .thenReturn(Optional.empty());
        when(watchedAnimeRepository.save(any(UserWatchedAnime.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserWatchedAnime watched = service.markWatched(
                account,
                " Attack-On-Titan ",
                Instant.parse("2026-05-20T19:00:00Z"),
                " trakt ");

        assertThat(watched.getExternalAccount()).isSameAs(account);
        assertThat(watched.getAnimeSlug()).isEqualTo("attack-on-titan");
        assertThat(watched.getSource()).isEqualTo("TRAKT");
        assertThat(watched.getWatchedAt()).isEqualTo("2026-05-20T19:00:00Z");
        assertThat(watched.getCreatedAt()).isEqualTo(NOW);
        verify(watchedAnimeRepository).save(watched);
    }

    @Test
    void markWatchedUpdatesExistingRowForSameAccountSlugAndSource() {
        ExternalAccount account = new ExternalAccount();
        UserWatchedAnime existing = new UserWatchedAnime();
        existing.setExternalAccount(account);
        existing.setAnimeSlug("attack-on-titan");
        existing.setSource("TRAKT");
        existing.setCreatedAt(Instant.parse("2026-05-20T19:00:00Z"));

        when(watchedAnimeRepository.findByExternalAccountAndAnimeSlugAndSource(
                account,
                "attack-on-titan",
                "TRAKT"))
                .thenReturn(Optional.of(existing));
        when(watchedAnimeRepository.save(any(UserWatchedAnime.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserWatchedAnime watched = service.markWatched(
                account,
                "attack-on-titan",
                Instant.parse("2026-05-21T19:00:00Z"),
                "trakt");

        assertThat(watched).isSameAs(existing);
        assertThat(watched.getWatchedAt()).isEqualTo("2026-05-21T19:00:00Z");
        assertThat(watched.getCreatedAt()).isEqualTo("2026-05-20T19:00:00Z");
    }

    @Test
    void markRatingStoresRatingOnExistingWatchedAnime() {
        ExternalAccount account = new ExternalAccount();
        UserWatchedAnime existing = new UserWatchedAnime();
        existing.setExternalAccount(account);
        existing.setAnimeSlug("attack-on-titan");
        existing.setSource("TRAKT");
        existing.setWatchedAt(Instant.parse("2026-05-20T19:00:00Z"));
        existing.setCreatedAt(Instant.parse("2026-05-20T19:00:00Z"));

        when(watchedAnimeRepository.findByExternalAccountAndAnimeSlugAndSource(
                account,
                "attack-on-titan",
                "TRAKT"))
                .thenReturn(Optional.of(existing));
        when(watchedAnimeRepository.save(any(UserWatchedAnime.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserWatchedAnime watched = service.markRating(
                account,
                "attack-on-titan",
                9,
                Instant.parse("2026-05-22T19:00:00Z"),
                "trakt");

        assertThat(watched.getRating()).isEqualTo(9);
        assertThat(watched.getRatedAt()).isEqualTo("2026-05-22T19:00:00Z");
        assertThat(watched.getWatchedAt()).isEqualTo("2026-05-20T19:00:00Z");
    }
}
