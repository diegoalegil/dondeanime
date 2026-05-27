package com.dondeanime.backend.curated;

public record CuratedListItemDto(
        String animeSlug,
        int position,
        String note
) {
    public static CuratedListItemDto from(CuratedListItem item) {
        return new CuratedListItemDto(
                item.getAnimeSlug(),
                item.getPosition(),
                item.getNote());
    }
}
