package com.dondeanime.backend.chat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;
import com.dondeanime.backend.anime.AnimeSummaryDto;
import com.dondeanime.backend.embedding.EmbeddingClient;
import com.dondeanime.backend.embedding.EmbeddingStorageService;
import com.dondeanime.backend.embedding.EmbeddingVector;
import com.dondeanime.backend.embedding.StoredAnimeEmbedding;
import com.dondeanime.backend.provider.WatchProvider;
import com.dondeanime.backend.provider.WatchProviderRepository;
import com.dondeanime.backend.subscription.CountryCatalog;

@Service
public class ChatSearchService {

    private static final int MAX_QUESTION_LENGTH = 500;
    private static final int CANDIDATE_LIMIT = 50;
    private static final int RESULT_LIMIT = 5;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern OBVIOUS_INJECTION = Pattern.compile(
            "(?iu)(ignore|ignora|olvida).{0,50}(instrucciones|previous|anteriores)|"
                    + "(system prompt|developer message|prompt injection|jailbreak|actua como|act as)");

    private final EmbeddingClient embeddingClient;
    private final EmbeddingStorageService storageService;
    private final AnimeRepository animeRepository;
    private final WatchProviderRepository providerRepository;
    private final String siteUrl;

    public ChatSearchService(
            EmbeddingClient embeddingClient,
            EmbeddingStorageService storageService,
            AnimeRepository animeRepository,
            WatchProviderRepository providerRepository,
            @Value("${dondeanime.site-url:https://dondeanime.com}") String siteUrl) {
        this.embeddingClient = embeddingClient;
        this.storageService = storageService;
        this.animeRepository = animeRepository;
        this.providerRepository = providerRepository;
        this.siteUrl = trimTrailingSlash(siteUrl);
    }

    public ChatSearchResponse search(ChatSearchRequest request) {
        String question = validateQuestion(request == null ? null : request.question());
        String countryCode = normalizeOptionalCountry(request == null ? null : request.countryCode());

        EmbeddingVector query = embeddingClient.embed(question);
        List<ScoredEmbedding> scored = storageService.findByModel(embeddingClient.model()).stream()
                .map(stored -> score(stored, query.values()))
                .filter(scoredEmbedding -> scoredEmbedding.score() > 0)
                .sorted(Comparator
                        .comparingDouble(ScoredEmbedding::score)
                        .reversed()
                        .thenComparing(scoredEmbedding -> scoredEmbedding.stored().animeId()))
                .limit(CANDIDATE_LIMIT)
                .toList();

        if (scored.isEmpty()) {
            return emptyResponse();
        }

        List<Long> animeIds = scored.stream()
                .map(scoredEmbedding -> scoredEmbedding.stored().animeId())
                .distinct()
                .toList();
        Map<Long, Anime> animeById = animeById(animeIds);
        Map<Long, List<WatchProvider>> providersByAnimeId = providersByAnimeId(animeIds);

        List<ChatRecommendationDto> recommendations = new ArrayList<>();
        for (ScoredEmbedding scoredEmbedding : scored) {
            Anime anime = animeById.get(scoredEmbedding.stored().animeId());
            if (anime == null) {
                continue;
            }

            List<WatchProvider> matchingProviders = matchingProviders(
                    providersByAnimeId.getOrDefault(anime.getId(), List.of()),
                    countryCode);
            if (countryCode != null && matchingProviders.isEmpty()) {
                continue;
            }

            recommendations.add(new ChatRecommendationDto(
                    AnimeSummaryDto.from(anime),
                    canonicalUrl(anime.getSlug()),
                    explanation(countryCode, matchingProviders)));
            if (recommendations.size() >= RESULT_LIMIT) {
                break;
            }
        }

        if (recommendations.isEmpty()) {
            return emptyResponse();
        }

        return new ChatSearchResponse(
                "He encontrado %d recomendaciones del catalogo de DondeAnime.".formatted(recommendations.size()),
                recommendations);
    }

    private static ScoredEmbedding score(StoredAnimeEmbedding stored, List<Double> queryVector) {
        return new ScoredEmbedding(stored, cosine(queryVector, stored.embedding()));
    }

    private Map<Long, Anime> animeById(List<Long> animeIds) {
        Map<Long, Anime> animeById = new LinkedHashMap<>();
        animeRepository.findAllById(animeIds).forEach(anime -> animeById.put(anime.getId(), anime));
        return animeById;
    }

    private Map<Long, List<WatchProvider>> providersByAnimeId(List<Long> animeIds) {
        if (animeIds.isEmpty()) {
            return Map.of();
        }
        return providerRepository
                .findByAnimeIdInOrderByAnimeIdAscCountryCodeAscProviderTypeAscProviderNameAsc(animeIds)
                .stream()
                .collect(Collectors.groupingBy(WatchProvider::getAnimeId));
    }

    private static List<WatchProvider> matchingProviders(List<WatchProvider> providers, String countryCode) {
        if (countryCode == null) {
            return providers;
        }
        return providers.stream()
                .filter(provider -> countryCode.equals(provider.getCountryCode()))
                .toList();
    }

    private static String explanation(String countryCode, List<WatchProvider> providers) {
        if (countryCode == null || providers.isEmpty()) {
            return "Coincide por similitud semantica con tu busqueda y existe en el catalogo.";
        }

        String providerNames = providers.stream()
                .map(WatchProvider::getProviderName)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", "));
        return "Coincide con tu busqueda y esta disponible en %s en %s."
                .formatted(CountryCatalog.countryName(countryCode), providerNames);
    }

    private ChatSearchResponse emptyResponse() {
        return new ChatSearchResponse(
                "No he encontrado recomendaciones fiables en el catalogo actual.",
                List.of());
    }

    private String canonicalUrl(String slug) {
        return siteUrl + "/anime/" + slug;
    }

    private static double cosine(List<Double> left, List<Double> right) {
        if (left == null || right == null || left.isEmpty() || left.size() != right.size()) {
            return 0;
        }

        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int i = 0; i < left.size(); i++) {
            double a = left.get(i);
            double b = right.get(i);
            dot += a * b;
            leftNorm += a * a;
            rightNorm += b * b;
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private static String validateQuestion(String question) {
        if (question == null || question.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question es obligatoria");
        }

        String normalized = WHITESPACE.matcher(question.trim()).replaceAll(" ");
        if (normalized.length() > MAX_QUESTION_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question es demasiado larga");
        }
        if (OBVIOUS_INJECTION.matcher(normalized).find()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question contiene instrucciones no permitidas");
        }
        return normalized;
    }

    private static String normalizeOptionalCountry(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return null;
        }
        return CountryCatalog.normalizeCountry(countryCode);
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://dondeanime.com";
        }
        String trimmed = value.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private record ScoredEmbedding(
            StoredAnimeEmbedding stored,
            double score) {
    }
}
