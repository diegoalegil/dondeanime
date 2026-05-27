package com.dondeanime.backend.curated;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CuratedListSchemaItemDto(
        @JsonProperty("@type") String type,
        int position,
        String name,
        String url
) {
    public static CuratedListSchemaItemDto from(CuratedListItemDto item, String siteUrl) {
        String title = item.anime() == null
                ? item.animeSlug()
                : titleOrSlug(item);
        return new CuratedListSchemaItemDto(
                "ListItem",
                item.position(),
                title,
                siteUrl + "/anime/" + item.animeSlug());
    }

    private static String titleOrSlug(CuratedListItemDto item) {
        if (item.anime().titleEnglish() != null && !item.anime().titleEnglish().isBlank()) {
            return item.anime().titleEnglish();
        }
        if (item.anime().titleRomaji() != null && !item.anime().titleRomaji().isBlank()) {
            return item.anime().titleRomaji();
        }
        return item.animeSlug();
    }
}
