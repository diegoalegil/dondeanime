package com.dondeanime.backend.embedding;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;
import com.dondeanime.backend.embedding.AnimeSearchDocument.CountryPlatforms;
import com.dondeanime.backend.provider.WatchProvider;
import com.dondeanime.backend.provider.WatchProviderRepository;

@Service
public class EmbeddingDocumentBuilder {

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Comparator<String> TEXT_ORDER =
            String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder());
    private static final Comparator<Anime> ANIME_ORDER = Comparator
            .comparing(EmbeddingDocumentBuilder::animeSortKey, TEXT_ORDER);

    private final AnimeRepository animeRepository;
    private final WatchProviderRepository providerRepository;
    private final String siteUrl;

    public EmbeddingDocumentBuilder(
            AnimeRepository animeRepository,
            WatchProviderRepository providerRepository,
            @Value("${dondeanime.site-url:https://dondeanime.com}") String siteUrl) {
        this.animeRepository = animeRepository;
        this.providerRepository = providerRepository;
        this.siteUrl = trimTrailingSlash(siteUrl);
    }

    public List<AnimeSearchDocument> buildDocuments() {
        return buildDocumentRecords().stream()
                .map(EmbeddingDocumentRecord::document)
                .toList();
    }

    public List<EmbeddingDocumentRecord> buildDocumentRecords() {
        List<Anime> anime = animeRepository.findAllWithGenres().stream()
                .filter(item -> item.getId() != null)
                .sorted(ANIME_ORDER)
                .toList();
        Map<Long, List<WatchProvider>> providersByAnimeId = providersByAnimeId(anime);

        return anime.stream()
                .map(item -> new EmbeddingDocumentRecord(
                        item.getId(),
                        buildDocument(item, providersByAnimeId.getOrDefault(item.getId(), List.of()))))
                .toList();
    }

    AnimeSearchDocument buildDocument(Anime anime, List<WatchProvider> providers) {
        String slug = cleanText(anime.getSlug());
        return new AnimeSearchDocument(
                slug,
                title(anime),
                cleanText(firstNonBlank(anime.getDescriptionEs(), anime.getDescription())),
                sortedGenres(anime.getGenres()),
                cleanText(anime.getSeason()),
                anime.getSeasonYear(),
                anime.getAverageScore(),
                availability(providers),
                canonicalUrl(slug));
    }

    private Map<Long, List<WatchProvider>> providersByAnimeId(List<Anime> anime) {
        List<Long> animeIds = anime.stream()
                .map(Anime::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (animeIds.isEmpty()) {
            return Map.of();
        }

        return providerRepository
                .findByAnimeIdInOrderByAnimeIdAscCountryCodeAscProviderTypeAscProviderNameAsc(animeIds)
                .stream()
                .collect(Collectors.groupingBy(WatchProvider::getAnimeId));
    }

    private List<CountryPlatforms> availability(List<WatchProvider> providers) {
        Map<String, TreeSet<String>> byCountry = new TreeMap<>();
        for (WatchProvider provider : providers) {
            String countryCode = cleanText(provider.getCountryCode()).toUpperCase(Locale.ROOT);
            String providerName = cleanText(provider.getProviderName());
            if (countryCode.isBlank() || providerName.isBlank()) {
                continue;
            }
            byCountry.computeIfAbsent(countryCode, ignored -> new TreeSet<>(TEXT_ORDER))
                    .add(providerName);
        }

        return byCountry.entrySet().stream()
                .map(entry -> new CountryPlatforms(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
    }

    private static List<String> sortedGenres(Set<String> genres) {
        if (genres == null || genres.isEmpty()) {
            return List.of();
        }

        return genres.stream()
                .map(EmbeddingDocumentBuilder::cleanText)
                .filter(genre -> !genre.isBlank())
                .distinct()
                .sorted(TEXT_ORDER)
                .toList();
    }

    private static String title(Anime anime) {
        return cleanText(firstNonBlank(anime.getTitleEnglish(), anime.getTitleRomaji(), anime.getSlug()));
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            String cleaned = cleanText(value);
            if (!cleaned.isBlank()) {
                return cleaned;
            }
        }
        return "";
    }

    private static String cleanText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return HTML_TAG.matcher(value)
                .replaceAll(" ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String canonicalUrl(String slug) {
        if (slug.isBlank()) {
            return siteUrl + "/anime";
        }
        return siteUrl + "/anime/" + slug;
    }

    private static String animeSortKey(Anime anime) {
        return firstNonBlank(anime.getSlug(), anime.getTitleEnglish(), anime.getTitleRomaji());
    }

    private static String trimTrailingSlash(String value) {
        String cleaned = cleanText(value);
        if (cleaned.endsWith("/")) {
            return cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned.isBlank() ? "https://dondeanime.com" : cleaned;
    }
}
