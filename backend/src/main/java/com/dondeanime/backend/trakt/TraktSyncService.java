package com.dondeanime.backend.trakt;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;

@Service
public class TraktSyncService {

    private static final String PROVIDER = "trakt";
    private static final String SOURCE = "TRAKT";

    private final TraktHistoryClient traktHistoryClient;
    private final ExternalAccountRepository accountRepository;
    private final ExternalAccountService externalAccountService;
    private final TraktSyncEventRepository syncEventRepository;
    private final AnimeRepository animeRepository;
    private final Clock clock;

    public TraktSyncService(
            TraktHistoryClient traktHistoryClient,
            ExternalAccountRepository accountRepository,
            ExternalAccountService externalAccountService,
            TraktSyncEventRepository syncEventRepository,
            AnimeRepository animeRepository,
            Clock clock) {
        this.traktHistoryClient = traktHistoryClient;
        this.accountRepository = accountRepository;
        this.externalAccountService = externalAccountService;
        this.syncEventRepository = syncEventRepository;
        this.animeRepository = animeRepository;
        this.clock = clock;
    }

    public TraktSyncResponse sync(TraktSyncRequest request) {
        String externalUserId = requireTrimmed(request.externalUserId(), "externalUserId");
        String accessToken = requireTrimmed(request.accessToken(), "accessToken");

        ExternalAccount account = accountRepository
                .findByProviderAndExternalUserId(PROVIDER, externalUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "external_account_not_found"));

        List<Anime> catalog = animeRepository.findAll();
        List<TraktUnmatchedItem> unmatched = new ArrayList<>();

        int watchedImported = 0;
        for (TraktWatchedShow watchedShow : safeList(traktHistoryClient.fetchWatchedShows(accessToken))) {
            Optional<Anime> match = match(catalog, watchedShow.show());
            if (match.isPresent()) {
                externalAccountService.markWatched(
                        account,
                        match.get().getSlug(),
                        watchedShow.lastWatchedAt() == null ? Instant.now(clock) : watchedShow.lastWatchedAt(),
                        SOURCE);
                watchedImported += 1;
            } else {
                unmatched.add(unmatched("WATCHED", watchedShow.show()));
            }
        }

        int ratingsImported = 0;
        for (TraktRatedShow ratedShow : safeList(traktHistoryClient.fetchRatedShows(accessToken))) {
            Optional<Anime> match = match(catalog, ratedShow.show());
            if (match.isPresent()) {
                externalAccountService.markRating(
                        account,
                        match.get().getSlug(),
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
