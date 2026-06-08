package com.dondeanime.backend.affiliate;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AffiliateLinkRepository extends JpaRepository<AffiliateLink, Long> {

    Optional<AffiliateLink> findByProviderSlugAndCountryCode(String providerSlug, String countryCode);

    Optional<AffiliateLink> findByProviderSlugAndCountryCodeAndActiveTrue(String providerSlug, String countryCode);

    List<AffiliateLink> findByProviderSlugInAndCountryCodeInAndActiveTrue(
            Collection<String> providerSlugs,
            Collection<String> countryCodes);

    List<AffiliateLink> findAllByOrderByProviderSlugAscCountryCodeAsc();

    List<AffiliateLink> findTop10ByOrderByClickCountDescProviderSlugAscCountryCodeAsc();

    @Modifying
    @Query("""
            UPDATE AffiliateLink a
               SET a.clickCount = a.clickCount + 1,
                   a.updatedAt = :now
             WHERE a.providerSlug = :providerSlug
               AND a.countryCode = :countryCode
               AND a.active = true
            """)
    int incrementClickCount(String providerSlug, String countryCode, Instant now);
}
