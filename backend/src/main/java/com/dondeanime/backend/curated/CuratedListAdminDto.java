package com.dondeanime.backend.curated;

import java.util.List;

public record CuratedListAdminDto(
        String slug,
        String title,
        String description,
        String owner,
        String visibility,
        String status,
        List<CuratedListItemDto> items
) {
    public static CuratedListAdminDto from(CuratedList list, List<CuratedListItemDto> items) {
        return new CuratedListAdminDto(
                list.getSlug(),
                list.getTitle(),
                list.getDescription(),
                list.getOwner(),
                list.getVisibility().name(),
                list.getStatus().name(),
                items);
    }
}
