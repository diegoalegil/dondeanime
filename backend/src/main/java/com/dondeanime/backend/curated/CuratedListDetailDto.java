package com.dondeanime.backend.curated;

import java.util.List;

public record CuratedListDetailDto(
        String slug,
        String title,
        String description,
        String owner,
        String visibility,
        String status,
        List<CuratedListItemDto> items
) {
    public static CuratedListDetailDto from(CuratedList list) {
        return new CuratedListDetailDto(
                list.getSlug(),
                list.getTitle(),
                list.getDescription(),
                CuratedListSummaryDto.publicOwner(list.getOwner()),
                list.getVisibility().name(),
                list.getStatus().name(),
                list.orderedItems().stream()
                        .map(CuratedListItemDto::from)
                        .toList());
    }
}
