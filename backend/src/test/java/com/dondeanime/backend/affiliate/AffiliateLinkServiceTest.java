package com.dondeanime.backend.affiliate;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.dondeanime.backend.AbstractIntegrationTest;

@SpringBootTest
class AffiliateLinkServiceTest extends AbstractIntegrationTest {

    @Autowired
    private AffiliateLinkRepository linkRepository;

    @Autowired
    private AffiliateClickEventRepository clickEventRepository;

    @Autowired
    private AffiliateLinkService service;

    @BeforeEach
    void cleanDatabase() {
        clickEventRepository.deleteAll();
        linkRepository.deleteAll();
    }

    @Test
    void saveLinkNormalizesProviderAndCountry() {
        AffiliateLinkDto dto = service.saveLink(new AffiliateLinkRequest(
                " Crunchyroll ",
                "es",
                "https://example.com/cr",
                true));

        assertThat(dto.providerSlug()).isEqualTo("crunchyroll");
        assertThat(dto.countryCode()).isEqualTo("ES");
        assertThat(dto.affiliateUrl()).isEqualTo("https://example.com/cr");
        assertThat(dto.active()).isTrue();

        Optional<AffiliateLink> stored = linkRepository.findByProviderSlugAndCountryCode("crunchyroll", "ES");
        assertThat(stored)
                .isPresent()
                .get()
                .extracting(AffiliateLink::getClickCount)
                .isEqualTo(0);
    }

    @Test
    void trackClickIncrementsLinkAndStoresEvent() {
        linkRepository.saveAndFlush(link());

        service.trackClick(new AffiliateTrackRequest(
                "Crunchyroll",
                "es",
                "attack-on-titan"));

        AffiliateLink stored = linkRepository.findByProviderSlugAndCountryCode("crunchyroll", "ES")
                .orElseThrow();
        assertThat(stored.getClickCount()).isEqualTo(1);
        assertThat(clickEventRepository.findAll())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getAffiliateLinkId()).isEqualTo(stored.getId());
                    assertThat(event.getProviderSlug()).isEqualTo("crunchyroll");
                    assertThat(event.getCountryCode()).isEqualTo("ES");
                    assertThat(event.getAnimeSlug()).isEqualTo("attack-on-titan");
                });
    }

    @Test
    void trackClickIsNoOpWhenLinkDoesNotExist() {
        service.trackClick(new AffiliateTrackRequest(
                "crunchyroll",
                "ES",
                "attack-on-titan"));

        assertThat(linkRepository.findAll()).isEmpty();
        assertThat(clickEventRepository.findAll()).isEmpty();
    }

    private static AffiliateLink link() {
        AffiliateLink link = new AffiliateLink();
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
