package com.dondeanime.backend.affiliate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dondeanime.backend.provider.ProviderDto;
import com.dondeanime.backend.provider.ProviderSummaryDto;
import com.dondeanime.backend.provider.WatchProvider;

@Service
public class AffiliateLinkService {

    private final AffiliateLinkRepository linkRepository;
    private final AffiliateClickEventRepository clickEventRepository;
    private final PlausibleStatsClient plausibleStatsClient;

    public AffiliateLinkService(
            AffiliateLinkRepository linkRepository,
            AffiliateClickEventRepository clickEventRepository,
            PlausibleStatsClient plausibleStatsClient) {
        this.linkRepository = linkRepository;
        this.clickEventRepository = clickEventRepository;
        this.plausibleStatsClient = plausibleStatsClient;
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
}
