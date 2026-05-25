package com.dondeanime.backend.affiliate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dondeanime.backend.provider.ProviderDto;
import com.dondeanime.backend.provider.ProviderSummaryDto;
import com.dondeanime.backend.provider.WatchProvider;
import com.dondeanime.backend.provider.WatchProviderRepository;

@Service
public class AffiliateLinkService {

    private final AffiliateLinkRepository linkRepository;
    private final AffiliateClickEventRepository clickEventRepository;
    private final PlausibleStatsClient plausibleStatsClient;
    private final WatchProviderRepository watchProviderRepository;

    public AffiliateLinkService(
            AffiliateLinkRepository linkRepository,
            AffiliateClickEventRepository clickEventRepository,
            PlausibleStatsClient plausibleStatsClient,
            WatchProviderRepository watchProviderRepository) {
        this.linkRepository = linkRepository;
        this.clickEventRepository = clickEventRepository;
        this.plausibleStatsClient = plausibleStatsClient;
        this.watchProviderRepository = watchProviderRepository;
    }

    @Transactional(readOnly = true)
    public List<AffiliateLinkDto> listLinks() {
        return linkRepository.findAllByOrderByProviderSlugAscCountryCodeAsc()
                .stream()
                .map(AffiliateLinkDto::from)
                .toList();
    }

    @Transactional
    public AffiliateLinkDto saveLink(AffiliateLinkRequest request) {
        String providerSlug = normalizeProviderSlug(request.providerSlug());
        String countryCode = normalizeCountry(request.country());
        Instant now = Instant.now();

        AffiliateLink link = linkRepository.findByProviderSlugAndCountryCode(providerSlug, countryCode)
                .orElseGet(() -> {
                    AffiliateLink created = new AffiliateLink();
                    created.setProviderSlug(providerSlug);
                    created.setCountryCode(countryCode);
                    created.setClickCount(0);
                    created.setCreatedAt(now);
                    return created;
                });

        link.setAffiliateUrl(request.affiliateUrl().trim());
        link.setActive(request.active() == null || request.active());
        link.setUpdatedAt(now);

        return AffiliateLinkDto.from(linkRepository.save(link));
    }

    @Transactional
    public AffiliateBulkImportResult bulkImport(String csv) {
        List<BulkAffiliateRow> rows = parseBulkCsv(csv);
        List<AffiliateBulkImportError> errors = validateBulkRows(rows);
        if (!errors.isEmpty()) {
            throw new AffiliateBulkImportException(errors);
        }

        List<AffiliateLinkDto> saved = rows.stream()
                .map(row -> saveLink(new AffiliateLinkRequest(
                        row.providerSlug(),
                        row.countryCode(),
                        row.url(),
                        row.active())))
                .toList();
        return new AffiliateBulkImportResult(saved.size(), saved);
    }

    @Transactional
    public void deleteLink(Long id) {
        linkRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ProviderDto toProviderDto(WatchProvider provider) {
        String providerSlug = ProviderSummaryDto.slugify(provider.getProviderName());
        String affiliateUrl = linkRepository
                .findByProviderSlugAndCountryCodeAndActiveTrue(providerSlug, provider.getCountryCode())
                .map(AffiliateLink::getAffiliateUrl)
                .orElse(null);
        return ProviderDto.from(provider, affiliateUrl);
    }

    @Transactional
    public void trackClick(AffiliateTrackRequest request) {
        String providerSlug = normalizeProviderSlug(request.providerSlug());
        String countryCode = normalizeCountry(request.country());
        String animeSlug = normalizeAnimeSlug(request.animeSlug());
        Instant now = Instant.now();

        Optional<AffiliateLink> link = linkRepository
                .findByProviderSlugAndCountryCodeAndActiveTrue(providerSlug, countryCode);
        if (link.isEmpty()) {
            return;
        }

        int updated = linkRepository.incrementClickCount(providerSlug, countryCode, now);
        if (updated == 0) {
            return;
        }

        AffiliateClickEvent event = new AffiliateClickEvent();
        event.setAffiliateLinkId(link.get().getId());
        event.setProviderSlug(providerSlug);
        event.setCountryCode(countryCode);
        event.setAnimeSlug(animeSlug);
        event.setClickedAt(now);
        clickEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public AffiliateDashboardDto dashboard() {
        Instant now = Instant.now();
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);

        List<AffiliateAnimeClicksDto> topAnime = clickEventRepository
                .findTopAnimeClicks(thirtyDaysAgo, PageRequest.of(0, 10))
                .stream()
                .map(row -> new AffiliateAnimeClicksDto(row.getAnimeSlug(), row.getClicks()))
                .toList();

        List<AffiliateLinkDto> topLinks = linkRepository.findTop10ByOrderByClickCountDescProviderSlugAscCountryCodeAsc()
                .stream()
                .map(AffiliateLinkDto::from)
                .toList();

        return new AffiliateDashboardDto(
                clickEventRepository.countByClickedAtAfter(sevenDaysAgo),
                clickEventRepository.countByClickedAtAfter(thirtyDaysAgo),
                topAnime,
                topLinks,
                plausibleStatsClient.topAnimePages30Days());
    }

    static String normalizeProviderSlug(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    static String normalizeCountry(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeAnimeSlug(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private List<BulkAffiliateRow> parseBulkCsv(String csv) {
        List<BulkAffiliateRow> rows = new ArrayList<>();
        String[] lines = csv == null ? new String[0] : csv.split("\\R");
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index].trim();
            if (line.isBlank()) {
                continue;
            }

            List<String> columns = splitCsvLine(line);
            if (index == 0 && columns.size() == 4 && "provider_slug".equalsIgnoreCase(columns.get(0).trim())) {
                continue;
            }
            rows.add(new BulkAffiliateRow(index + 1, columns));
        }
        return rows;
    }

    private List<AffiliateBulkImportError> validateBulkRows(List<BulkAffiliateRow> rows) {
        List<AffiliateBulkImportError> errors = new ArrayList<>();
        Set<String> providerCountries = existingProviderCountries();

        for (BulkAffiliateRow row : rows) {
            if (row.columns().size() != 4) {
                errors.add(new AffiliateBulkImportError(row.line(), "Se esperaban 4 columnas"));
                continue;
            }

            String providerSlug = normalizeProviderSlug(row.providerSlug());
            String countryCode = normalizeCountry(row.countryCode());
            String url = row.url().trim();

            if (providerSlug.isBlank()) {
                errors.add(new AffiliateBulkImportError(row.line(), "provider_slug vacío"));
            }
            if (!countryCode.matches("[A-Z]{2}")) {
                errors.add(new AffiliateBulkImportError(row.line(), "country_code debe tener 2 letras"));
            }
            if (!url.matches("^https?://.+")) {
                errors.add(new AffiliateBulkImportError(row.line(), "url debe empezar por http:// o https://"));
            }
            if (!row.hasValidActive()) {
                errors.add(new AffiliateBulkImportError(row.line(), "active debe ser true/false"));
            }
            if (!providerCountries.contains(providerSlug + "|" + countryCode)) {
                errors.add(new AffiliateBulkImportError(row.line(), "provider_slug+country_code no existe en catálogo"));
            }
        }

        return errors;
    }

    private Set<String> existingProviderCountries() {
        Set<String> providerCountries = new HashSet<>();
        for (WatchProviderRepository.ProviderCountryAggregation providerCountry
                : watchProviderRepository.aggregateProviderCountries()) {
            providerCountries.add(ProviderSummaryDto.slugify(providerCountry.getProviderName())
                    + "|"
                    + normalizeCountry(providerCountry.getCountryCode()));
        }
        return providerCountries;
    }

    private static List<String> splitCsvLine(String line) {
        List<String> columns = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (character == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (character == ',' && !inQuotes) {
                columns.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }

        columns.add(current.toString().trim());
        return columns;
    }

    private record BulkAffiliateRow(int line, List<String> columns) {

        String providerSlug() {
            return columns.get(0);
        }

        String countryCode() {
            return columns.get(1);
        }

        String url() {
            return columns.get(2);
        }

        Boolean active() {
            return parseActive(columns.get(3));
        }

        boolean hasValidActive() {
            return active() != null;
        }

        private static Boolean parseActive(String value) {
            String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "true", "1", "yes", "si", "sí" -> true;
                case "false", "0", "no" -> false;
                default -> null;
            };
        }
    }
}
