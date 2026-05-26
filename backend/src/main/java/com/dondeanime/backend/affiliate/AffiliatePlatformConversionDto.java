package com.dondeanime.backend.affiliate;

public record AffiliatePlatformConversionDto(
        String providerSlug,
        Long clicks,
        Long detailViews,
        Double conversionRate
) {}
