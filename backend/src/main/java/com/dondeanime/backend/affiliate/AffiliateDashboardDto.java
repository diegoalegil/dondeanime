package com.dondeanime.backend.affiliate;

import java.util.List;

import com.dondeanime.backend.anime.RecommendationClickDto;

public record AffiliateDashboardDto(
        Long clicksLast7Days,
        Long clicksLast30Days,
        List<AffiliateAnimeClicksDto> topAffiliateAnime,
        List<AffiliateLinkDto> topAffiliateLinks,
        List<PlausiblePageMetricDto> topVisitedAnime,
        List<RecommendationClickDto> topRecommendationClicks
) {}
