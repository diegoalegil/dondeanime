package com.dondeanime.backend.curated;

import java.util.List;

public record CuratedListDetailDto(
        String slug,
        String title,
        String description,
        String owner,
        String visibility,
        String status,
        boolean premiumOnly,
        boolean premiumPreview,
        String premiumCtaUrl,
        int itemCount,
        List<CuratedListItemDto> items,
        CuratedListItemListSchemaDto schema
) {
    public static CuratedListDetailDto from(CuratedList list) {
        List<CuratedListItemDto> items = list.orderedItems().stream()
                .map(CuratedListItemDto::from)
                .toList();
        return from(list, items, "https://dondeanime.com", false, null);
    }

    public static CuratedListDetailDto from(
            CuratedList list,
            List<CuratedListItemDto> items,
            String siteUrl,
            boolean premiumPreview,
            String premiumCtaUrl) {
        return new CuratedListDetailDto(
                list.getSlug(),
                list.getTitle(),
                list.getDescription(),
                CuratedListSummaryDto.publicOwner(list.getOwner()),
                list.getVisibility().name(),
                list.getStatus().name(),
                list.isPremiumOnly(),
                premiumPreview,
                premiumCtaUrl,
                list.getItems().size(),
                items,
                CuratedListItemListSchemaDto.from(list, items, siteUrl));
    }
}
