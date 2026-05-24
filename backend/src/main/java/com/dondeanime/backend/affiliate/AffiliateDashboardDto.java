package com.dondeanime.backend.affiliate;

import java.util.List;

public record AffiliateDashboardDto(
        Long clicksLast7Days,
        Long clicksLast30Days,
        List<AffiliateAnimeClicksDto> topAffiliateAnime,
        List<AffiliateLinkDto> topAffiliateLinks,
        List<PlausiblePageMetricDto> topVisitedAnime
) {}
