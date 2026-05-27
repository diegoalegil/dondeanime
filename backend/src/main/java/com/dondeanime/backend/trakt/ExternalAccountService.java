package com.dondeanime.backend.trakt;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExternalAccountService {

    private final ExternalAccountRepository accountRepository;
    private final UserWatchedAnimeRepository watchedAnimeRepository;
    private final Clock clock;

    public ExternalAccountService(
            ExternalAccountRepository accountRepository,
            UserWatchedAnimeRepository watchedAnimeRepository,
            Clock clock) {
        this.accountRepository = accountRepository;
        this.watchedAnimeRepository = watchedAnimeRepository;
        this.clock = clock;
    }

    @Transactional
    public ExternalAccount upsert(ExternalAccountUpsertCommand command) {
        String provider = normalizeProvider(command.provider());
        String externalUserId = requireTrimmed(command.externalUserId(), "externalUserId");
        Instant now = Instant.now(clock);

        ExternalAccount account = accountRepository
                .findByProviderAndExternalUserId(provider, externalUserId)
                .orElseGet(() -> {
                    ExternalAccount created = new ExternalAccount();
                    created.setProvider(provider);
                    created.setExternalUserId(externalUserId);
                    created.setCreatedAt(now);
                    return created;
                });

        account.setEmail(normalizeEmail(command.email()));
        account.setAccessTokenCiphertext(blankToNull(command.accessTokenCiphertext()));
        account.setRefreshTokenCiphertext(blankToNull(command.refreshTokenCiphertext()));
        account.setScopes(normalizeScopes(command.scopes()));
        account.setTokenExpiresAt(command.tokenExpiresAt());
        account.setUpdatedAt(now);
        return accountRepository.save(account);
    }

    @Transactional
    public UserWatchedAnime markWatched(
            ExternalAccount account,
            String animeSlug,
            Instant watchedAt,
            String source) {
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(watchedAt, "watchedAt");

        String normalizedSlug = requireTrimmed(animeSlug, "animeSlug").toLowerCase(Locale.ROOT);
        String normalizedSource = requireTrimmed(source, "source").toUpperCase(Locale.ROOT);

        UserWatchedAnime watched = watchedAnimeRepository
                .findByExternalAccountAndAnimeSlugAndSource(account, normalizedSlug, normalizedSource)
                .orElseGet(() -> {
                    UserWatchedAnime created = new UserWatchedAnime();
                    created.setExternalAccount(account);
                    created.setAnimeSlug(normalizedSlug);
                    created.setSource(normalizedSource);
                    created.setCreatedAt(Instant.now(clock));
                    return created;
                });

        watched.setWatchedAt(watchedAt);
        return watchedAnimeRepository.save(watched);
    }

    static String normalizeProvider(String provider) {
        return requireTrimmed(provider, "provider").toLowerCase(Locale.ROOT);
    }

    static String normalizeEmail(String email) {
        String value = blankToNull(email);
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    static String normalizeScopes(String scopes) {
        String value = blankToNull(scopes);
        if (value == null) {
            return null;
        }
        return Arrays.stream(value.split("[,\\s]+"))
                .map(String::trim)
                .filter(scope -> !scope.isBlank())
                .map(scope -> scope.toLowerCase(Locale.ROOT))
                .distinct()
                .sorted()
                .collect(Collectors.joining(" "));
    }

    private static String requireTrimmed(String value, String field) {
        String trimmed = blankToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return trimmed;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
