package com.dondeanime.backend.curated;

import java.util.List;

public record CuratedListDetailDto(
        String slug,
        String title,
        String description,
        String owner,
        String visibility,
        String status,
        List<CuratedListItemDto> items,
        CuratedListItemListSchemaDto schema
) {
    public static CuratedListDetailDto from(CuratedList list) {
        List<CuratedListItemDto> items = list.orderedItems().stream()
                .map(CuratedListItemDto::from)
                .toList();
        return from(list, items, "https://dondeanime.com");
    }

    public static CuratedListDetailDto from(
            CuratedList list,
            List<CuratedListItemDto> items,
            String siteUrl) {
        return new CuratedListDetailDto(
                list.getSlug(),
                list.getTitle(),
                list.getDescription(),
                CuratedListSummaryDto.publicOwner(list.getOwner()),
                list.getVisibility().name(),
                list.getStatus().name(),
                items,
                CuratedListItemListSchemaDto.from(list, items, siteUrl));
    }
}
