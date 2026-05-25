package com.dondeanime.backend.affiliate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.dondeanime.backend.provider.WatchProviderRepository;

class AffiliateLinkServiceTest {

    private final AffiliateLinkRepository linkRepository = org.mockito.Mockito.mock(AffiliateLinkRepository.class);
    private final AffiliateClickEventRepository clickEventRepository = org.mockito.Mockito.mock(AffiliateClickEventRepository.class);
    private final PlausibleStatsClient plausibleStatsClient = org.mockito.Mockito.mock(PlausibleStatsClient.class);
    private final WatchProviderRepository watchProviderRepository = org.mockito.Mockito.mock(WatchProviderRepository.class);

    private final AffiliateLinkService service = new AffiliateLinkService(
            linkRepository,
            clickEventRepository,
            plausibleStatsClient,
            watchProviderRepository);

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
}
