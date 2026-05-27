package com.dondeanime.backend.curated;

import com.dondeanime.backend.anime.AnimeSummaryDto;

public record CuratedListItemDto(
        String animeSlug,
        int position,
        String note,
        AnimeSummaryDto anime
) {
    public static CuratedListItemDto from(CuratedListItem item) {
        return from(item, null);
    }

    public static CuratedListItemDto from(CuratedListItem item, AnimeSummaryDto anime) {
        return new CuratedListItemDto(
                item.getAnimeSlug(),
                item.getPosition(),
                item.getNote(),
                anime);
    }
}
