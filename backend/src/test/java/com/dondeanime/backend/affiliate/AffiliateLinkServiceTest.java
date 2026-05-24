package com.dondeanime.backend.affiliate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class AffiliateLinkServiceTest {

    private final AffiliateLinkRepository linkRepository = org.mockito.Mockito.mock(AffiliateLinkRepository.class);
    private final AffiliateClickEventRepository clickEventRepository = org.mockito.Mockito.mock(AffiliateClickEventRepository.class);
    private final PlausibleStatsClient plausibleStatsClient = org.mockito.Mockito.mock(PlausibleStatsClient.class);

    private final AffiliateLinkService service = new AffiliateLinkService(
            linkRepository,
            clickEventRepository,
            plausibleStatsClient);

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
}
