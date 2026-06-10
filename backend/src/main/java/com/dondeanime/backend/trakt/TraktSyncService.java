package com.dondeanime.backend.trakt;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;

@Service
public class TraktSyncService {

    private static final String PROVIDER = "trakt";
    private static final String SOURCE = "TRAKT";
    private static final Duration TOKEN_REFRESH_SKEW = Duration.ofMinutes(1);

    private final TraktClient traktClient;
    private final TraktHistoryClient traktHistoryClient;
    private final ExternalAccountRepository accountRepository;
    private final ExternalAccountService externalAccountService;
    private final TraktSyncEventRepository syncEventRepository;
    private final AnimeRepository animeRepository;
    private final TraktTokenCipherService tokenCipherService;
    private final Clock clock;
    private final boolean enabled;

    public TraktSyncService(
            TraktClient traktClient,
            TraktHistoryClient traktHistoryClient,
            ExternalAccountRepository accountRepository,
            ExternalAccountService externalAccountService,
            TraktSyncEventRepository syncEventRepository,
            AnimeRepository animeRepository,
            TraktTokenCipherService tokenCipherService,
            Clock clock,
            @Value("${trakt.enabled:false}") boolean enabled) {
        this.traktClient = traktClient;
        this.traktHistoryClient = traktHistoryClient;
        this.accountRepository = accountRepository;
        this.externalAccountService = externalAccountService;
        this.syncEventRepository = syncEventRepository;
        this.animeRepository = animeRepository;
        this.tokenCipherService = tokenCipherService;
        this.clock = clock;
        this.enabled = enabled;
    }

    public TraktSyncResponse sync(String requestedExternalUserId) {
        ensureEnabled();
        String externalUserId = requireTrimmed(requestedExternalUserId, "externalUserId");

        ExternalAccount account = accountRepository
                .findByProviderAndExternalUserId(PROVIDER, externalUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "external_account_not_found"));
        String accessToken = accessTokenFor(account);

        List<Anime> catalog = animeRepository.findAll();
        List<TraktWatchedShow> watchedShows = fetchWatchedShows(accessToken);
        List<TraktRatedShow> ratedShows = fetchRatedShows(accessToken);
        List<TraktUnmatchedItem> unmatched = new ArrayList<>();

        int watchedImported = 0;
        Set<String> watchedSlugs = new HashSet<>();
        for (TraktWatchedShow watchedShow : watchedShows) {
            Optional<Anime> match = match(catalog, watchedShow.show());
            if (match.isPresent()) {
                String slug = match.get().getSlug();
                if (!watchedSlugs.add(slug)) {
                    continue;
                }
                externalAccountService.markWatched(
                        account,
                        slug,
                        watchedShow.lastWatchedAt() == null ? Instant.now(clock) : watchedShow.lastWatchedAt(),
                        SOURCE);
                watchedImported += 1;
            } else {
                unmatched.add(unmatched("WATCHED", watchedShow.show()));
            }
        }

        int ratingsImported = 0;
        Set<String> ratedSlugs = new HashSet<>();
        for (TraktRatedShow ratedShow : ratedShows) {
            Optional<Anime> match = match(catalog, ratedShow.show());
            if (match.isPresent()) {
                String slug = match.get().getSlug();
                if (!ratedSlugs.add(slug)) {
                    continue;
                }
                externalAccountService.markRating(
                        account,
                        slug,
                        ratedShow.rating(),
                        ratedShow.ratedAt() == null ? Instant.now(clock) : ratedShow.ratedAt(),
                        SOURCE);
                ratingsImported += 1;
            } else {
                unmatched.add(unmatched("RATING", ratedShow.show()));
            }
        }

        account.setLastSyncedAt(Instant.now(clock));
        accountRepository.save(account);
        saveSyncEvent(watchedImported, ratingsImported, unmatched.size());

        return new TraktSyncResponse(
                watchedImported,
                ratingsImported,
                unmatched.size(),
                unmatched.stream().limit(20).toList());
    }

    private String accessTokenFor(ExternalAccount account) {
        if (!tokenCipherService.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "trakt_oauth_not_configured");
        }
        if (shouldRefresh(account)) {
            return refreshAccessToken(account);
        }
        String accessToken = tokenCipherService.decrypt(account.getAccessTokenCiphertext());
        if (accessToken == null || accessToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "trakt_access_token_missing");
        }
        return accessToken;
    }

    private boolean shouldRefresh(ExternalAccount account) {
        return account.getAccessTokenCiphertext() == null
                || account.getAccessTokenCiphertext().isBlank()
                || account.getTokenExpiresAt() == null
                || !account.getTokenExpiresAt().isAfter(Instant.now(clock).plus(TOKEN_REFRESH_SKEW));
    }

    private String refreshAccessToken(ExternalAccount account) {
        String refreshToken = tokenCipherService.decrypt(account.getRefreshTokenCiphertext());
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "trakt_refresh_token_missing");
        }
        TraktOAuthTokenResponse token;
        try {
            token = traktClient.refreshAccessToken(refreshToken);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "trakt_refresh_failed", e);
        }
        if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "invalid_trakt_token_response");
        }
        String refreshCiphertext = token.refreshToken() == null || token.refreshToken().isBlank()
                ? account.getRefreshTokenCiphertext()
                : tokenCipherService.encrypt(token.refreshToken());
        externalAccountService.upsert(new ExternalAccountUpsertCommand(
                PROVIDER,
                account.getExternalUserId(),
                account.getEmail(),
                tokenCipherService.encrypt(token.accessToken()),
                refreshCiphertext,
                token.scope() == null ? account.getScopes() : token.scope(),
                expiresAt(token)));
        return token.accessToken();
    }

    private List<TraktWatchedShow> fetchWatchedShows(String accessToken) {
        try {
            return safeList(traktHistoryClient.fetchWatchedShows(accessToken));
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "trakt_sync_failed", e);
        }
    }

    private List<TraktRatedShow> fetchRatedShows(String accessToken) {
        try {
            return safeList(traktHistoryClient.fetchRatedShows(accessToken));
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "trakt_sync_failed", e);
        }
    }

    private Instant expiresAt(TraktOAuthTokenResponse token) {
        if (token.expiresIn() == null) {
            return null;
        }
        Instant issuedAt = token.createdAt() == null
                ? Instant.now(clock)
                : Instant.ofEpochSecond(token.createdAt());
        return issuedAt.plusSeconds(token.expiresIn());
    }

    private void ensureEnabled() {
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "trakt_disabled");
        }
    }

    private void saveSyncEvent(int watchedImported, int ratingsImported, int unmatchedCount) {
        TraktSyncEvent event = new TraktSyncEvent();
        event.setProvider(PROVIDER);
        event.setSyncedAt(Instant.now(clock));
        event.setWatchedImported(watchedImported);
        event.setRatingsImported(ratingsImported);
        event.setUnmatchedCount(unmatchedCount);
        syncEventRepository.save(event);
    }

    private static Optional<Anime> match(List<Anime> catalog, TraktShow show) {
        if (show == null || show.title() == null || show.year() == null) {
            return Optional.empty();
        }

        String traktTitle = normalizeTitle(show.title());
        return catalog.stream()
                .filter(anime -> Objects.equals(show.year(), animeYear(anime)))
                .filter(anime -> normalizeTitle(anime.getTitleEnglish()).equals(traktTitle)
                        || normalizeTitle(anime.getTitleRomaji()).equals(traktTitle))
                .findFirst();
    }

    private static Integer animeYear(Anime anime) {
        return anime.getStartYear() == null ? anime.getSeasonYear() : anime.getStartYear();
    }

    private static String normalizeTitle(String value) {
        if (value == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private static TraktUnmatchedItem unmatched(String type, TraktShow show) {
        return new TraktUnmatchedItem(
                type,
                show == null ? null : show.title(),
                show == null ? null : show.year(),
                "No existe match local por titulo y anio");
    }

    private static <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }

    private static String requireTrimmed(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + "_required");
        }
        return value.trim();
    }
}
