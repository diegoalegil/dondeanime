package com.dondeanime.backend.affiliate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.dondeanime.backend.anime.RecommendationEventRepository;
import com.dondeanime.backend.curated.CuratedListMetricRepository;
import com.dondeanime.backend.curated.CuratedListMetricType;
import com.dondeanime.backend.provider.AvailabilityChangeEventRepository;
import com.dondeanime.backend.provider.WatchProviderRepository;
import com.dondeanime.backend.trakt.TraktDashboardMetricsDto;
import com.dondeanime.backend.trakt.TraktDashboardMetricsService;

class AffiliateLinkServiceTest {

    private final AffiliateLinkRepository linkRepository = org.mockito.Mockito.mock(AffiliateLinkRepository.class);
    private final AffiliateClickEventRepository clickEventRepository = org.mockito.Mockito.mock(AffiliateClickEventRepository.class);
    private final PlausibleStatsClient plausibleStatsClient = org.mockito.Mockito.mock(PlausibleStatsClient.class);
    private final WatchProviderRepository watchProviderRepository = org.mockito.Mockito.mock(WatchProviderRepository.class);
    private final AvailabilityChangeEventRepository availabilityChangeEventRepository =
            org.mockito.Mockito.mock(AvailabilityChangeEventRepository.class);
    private final RecommendationEventRepository recommendationEventRepository =
            org.mockito.Mockito.mock(RecommendationEventRepository.class);
    private final CuratedListMetricRepository curatedListMetricRepository =
            org.mockito.Mockito.mock(CuratedListMetricRepository.class);
    private final TraktDashboardMetricsService traktDashboardMetricsService =
            org.mockito.Mockito.mock(TraktDashboardMetricsService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-25T12:00:00Z"), ZoneOffset.UTC);

    private final AffiliateLinkService service = new AffiliateLinkService(
            linkRepository,
            clickEventRepository,
            plausibleStatsClient,
            watchProviderRepository,
            availabilityChangeEventRepository,
            recommendationEventRepository,
            curatedListMetricRepository,
            traktDashboardMetricsService,
            clock);

    @Test
    void saveLinkNormalizesProviderAndCountry() {
        when(linkRepository.findByProviderSlugAndCountryCode("crunchyroll", "ES"))
                .thenReturn(Optional.empty());
        when(linkRepository.save(any(AffiliateLink.class))).thenAnswer(invocation -> {
            AffiliateLink link = invocation.getArgument(0);
            link.setId(1L);
            return link;
        });

        AffiliateLinkDto dto = service.saveLink(new AffiliateLinkRequest(
                " Crunchyroll ",
                "es",
                "https://example.com/cr",
                true));

        assertThat(dto.providerSlug()).isEqualTo("crunchyroll");
        assertThat(dto.countryCode()).isEqualTo("ES");
        assertThat(dto.affiliateUrl()).isEqualTo("https://example.com/cr");
        assertThat(dto.active()).isTrue();
    }

    @Test
    void trackClickIncrementsLinkAndStoresEvent() {
        AffiliateLink link = link();
        when(linkRepository.findByProviderSlugAndCountryCodeAndActiveTrue("crunchyroll", "ES"))
                .thenReturn(Optional.of(link));
        when(linkRepository.incrementClickCount(eq("crunchyroll"), eq("ES"), any(Instant.class)))
                .thenReturn(1);

        service.trackClick(new AffiliateTrackRequest(
                "Crunchyroll",
                "es",
                "attack-on-titan"));

        verify(linkRepository).incrementClickCount(eq("crunchyroll"), eq("ES"), any(Instant.class));
        verify(clickEventRepository).save(any(AffiliateClickEvent.class));
    }

    @Test
    void trackClickIsNoOpWhenLinkDoesNotExist() {
        when(linkRepository.findByProviderSlugAndCountryCodeAndActiveTrue("crunchyroll", "ES"))
                .thenReturn(Optional.empty());

        service.trackClick(new AffiliateTrackRequest(
                "crunchyroll",
                "ES",
                "attack-on-titan"));

        verify(linkRepository, never()).incrementClickCount(any(), any(), any());
        verify(clickEventRepository, never()).save(any());
    }

    @Test
    void bulkImportValidatesProviderCountryAndSavesRows() {
        when(watchProviderRepository.aggregateProviderCountries())
                .thenReturn(List.of(providerCountry("Crunchyroll", "ES")));
        when(linkRepository.findByProviderSlugAndCountryCode("crunchyroll", "ES"))
                .thenReturn(Optional.empty());
        when(linkRepository.save(any(AffiliateLink.class))).thenAnswer(invocation -> {
            AffiliateLink link = invocation.getArgument(0);
            link.setId(1L);
            return link;
        });

        AffiliateBulkImportResult result = service.bulkImport("""
                provider_slug,country_code,url,active
                crunchyroll,ES,https://example.com/cr,true
                """);

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.links().getFirst().providerSlug()).isEqualTo("crunchyroll");
    }

    @Test
    void bulkImportRejectsUnknownProviderCountry() {
        when(watchProviderRepository.aggregateProviderCountries())
                .thenReturn(List.of(providerCountry("Netflix", "ES")));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.bulkImport("""
                provider_slug,country_code,url,active
                crunchyroll,ES,https://example.com/cr,true
                """))
                .isInstanceOf(AffiliateBulkImportException.class)
                .extracting(exception -> ((AffiliateBulkImportException) exception).getErrors().getFirst().message())
                .isEqualTo("provider_slug+country_code no existe en catálogo");
    }

    @Test
    void dashboardReturnsExtendedMetrics() {
        when(clickEventRepository.countByClickedAtAfter(any(Instant.class))).thenReturn(7L, 30L);
        when(clickEventRepository.findTopAnimeClicks(any(Instant.class), any()))
                .thenReturn(List.of(animeClicks("frieren", 9L)));
        when(linkRepository.findTop10ByOrderByClickCountDescProviderSlugAscCountryCodeAsc())
                .thenReturn(List.of(link()));
        when(plausibleStatsClient.topAnimePages30Days())
                .thenReturn(List.of(new PlausiblePageMetricDto("/anime/frieren", 120L)));
        when(plausibleStatsClient.animeDetailPageviews30Days()).thenReturn(200L);
        when(clickEventRepository.countClicksByDay(any(Instant.class)))
                .thenReturn(List.of(dailyClicks(LocalDate.of(2026, 5, 24), 4L)));
        when(clickEventRepository.findTopProviderClicks(any(Instant.class), any()))
                .thenReturn(List.of(providerClicks("crunchyroll", 10L)));
        when(clickEventRepository.findTopCountryClicks(any(Instant.class), any()))
                .thenReturn(List.of(countryClicks("ES", 8L)));
        when(availabilityChangeEventRepository.findTopAnimeChanges(any(Instant.class), any()))
                .thenReturn(List.of(availabilityChanges("frieren", 3L)));
        when(recommendationEventRepository.findTopRecommendationClicks(any(Instant.class), any()))
                .thenReturn(List.of(recommendationClicks("frieren", "violet-evergarden", 5L)));
        when(curatedListMetricRepository.countByEventTypeAndOccurredAtAfter(eq(CuratedListMetricType.VIEW), any()))
                .thenReturn(11L);
        when(curatedListMetricRepository.countByEventTypeAndOccurredAtAfter(eq(CuratedListMetricType.ANIME_CLICK), any()))
                .thenReturn(6L);
        when(curatedListMetricRepository.countByEventTypeAndOccurredAtAfter(eq(CuratedListMetricType.PREMIUM_CTA_CLICK), any()))
                .thenReturn(2L);
        when(curatedListMetricRepository.countByEventTypeAndOccurredAtAfter(eq(CuratedListMetricType.PREMIUM_CONVERSION), any()))
                .thenReturn(1L);
        when(curatedListMetricRepository.findTopLists(any(), any(), any()))
                .thenReturn(List.of(listMetric("anime-para-empezar", 11L)));
        when(traktDashboardMetricsService.metrics())
                .thenReturn(new TraktDashboardMetricsDto(2L, 1L, 4L, 3L));

        AffiliateDashboardDto dashboard = service.dashboard();

        assertThat(dashboard.clicksLast7Days()).isEqualTo(7L);
        assertThat(dashboard.clicksLast30Days()).isEqualTo(30L);
        assertThat(dashboard.clicksByDay()).hasSize(30);
        assertThat(dashboard.clicksByDay().getLast().date()).isEqualTo(LocalDate.of(2026, 5, 25));
        assertThat(dashboard.clicksByDay().get(28).clicks()).isEqualTo(4L);
        assertThat(dashboard.platformConversions().getFirst().conversionRate()).isEqualTo(0.05);
        assertThat(dashboard.topClickCountries().getFirst().countryCode()).isEqualTo("ES");
        assertThat(dashboard.topAvailabilityChanges().getFirst().changes()).isEqualTo(3L);
        assertThat(dashboard.topRecommendationClicks().getFirst().targetAnimeSlug()).isEqualTo("violet-evergarden");
        assertThat(dashboard.curatedListViewsLast30Days()).isEqualTo(11L);
        assertThat(dashboard.curatedListPremiumClicksLast30Days()).isEqualTo(2L);
        assertThat(dashboard.curatedListPremiumConversionsLast30Days()).isEqualTo(1L);
        assertThat(dashboard.topCuratedLists().getFirst().listSlug()).isEqualTo("anime-para-empezar");
        assertThat(dashboard.trakt().connectedAccounts()).isEqualTo(2L);
        assertThat(dashboard.trakt().failedMatchesLast30Days()).isEqualTo(3L);
    }

    private static AffiliateLink link() {
        AffiliateLink link = new AffiliateLink();
        link.setId(10L);
        link.setProviderSlug("crunchyroll");
        link.setCountryCode("ES");
        link.setAffiliateUrl("https://example.com/cr");
        link.setClickCount(0);
        link.setActive(true);
        link.setCreatedAt(Instant.now());
        link.setUpdatedAt(Instant.now());
        return link;
    }

    private static WatchProviderRepository.ProviderCountryAggregation providerCountry(String providerName, String countryCode) {
        return new WatchProviderRepository.ProviderCountryAggregation() {
            @Override
            public String getProviderName() {
                return providerName;
            }

            @Override
            public String getCountryCode() {
                return countryCode;
            }
        };
    }

    private static AffiliateClickEventRepository.DailyClickProjection dailyClicks(LocalDate date, Long clicks) {
        return new AffiliateClickEventRepository.DailyClickProjection() {
            @Override
            public LocalDate getClickDate() {
                return date;
            }

            @Override
            public Long getClicks() {
                return clicks;
            }
        };
    }

    private static AffiliateClickEventRepository.AnimeClickProjection animeClicks(String animeSlug, Long clicks) {
        return new AffiliateClickEventRepository.AnimeClickProjection() {
            @Override
            public String getAnimeSlug() {
                return animeSlug;
            }

            @Override
            public Long getClicks() {
                return clicks;
            }
        };
    }

    private static AffiliateClickEventRepository.ProviderClickProjection providerClicks(String providerSlug, Long clicks) {
        return new AffiliateClickEventRepository.ProviderClickProjection() {
            @Override
            public String getProviderSlug() {
                return providerSlug;
            }

            @Override
            public Long getClicks() {
                return clicks;
            }
        };
    }

    private static AffiliateClickEventRepository.CountryClickProjection countryClicks(String countryCode, Long clicks) {
        return new AffiliateClickEventRepository.CountryClickProjection() {
            @Override
            public String getCountryCode() {
                return countryCode;
            }

            @Override
            public Long getClicks() {
                return clicks;
            }
        };
    }

    private static AvailabilityChangeEventRepository.AnimeAvailabilityChangeProjection availabilityChanges(
            String animeSlug,
            Long changes) {
        return new AvailabilityChangeEventRepository.AnimeAvailabilityChangeProjection() {
            @Override
            public String getAnimeSlug() {
                return animeSlug;
            }

            @Override
            public Long getChanges() {
                return changes;
            }
        };
    }

    private static RecommendationEventRepository.RecommendationClickProjection recommendationClicks(
            String sourceAnimeSlug,
            String targetAnimeSlug,
            Long clicks) {
        return new RecommendationEventRepository.RecommendationClickProjection() {
            @Override
            public String getSourceAnimeSlug() {
                return sourceAnimeSlug;
            }

            @Override
            public String getTargetAnimeSlug() {
                return targetAnimeSlug;
            }

            @Override
            public Long getClicks() {
                return clicks;
            }
        };
    }

    private static CuratedListMetricRepository.ListMetricProjection listMetric(String listSlug, Long events) {
        return new CuratedListMetricRepository.ListMetricProjection() {
            @Override
            public String getListSlug() {
                return listSlug;
            }

            @Override
            public Long getEvents() {
                return events;
            }
        };
    }
}
