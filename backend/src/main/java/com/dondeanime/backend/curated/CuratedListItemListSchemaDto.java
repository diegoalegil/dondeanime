package com.dondeanime.backend.curated;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CuratedListItemListSchemaDto(
        @JsonProperty("@context") String context,
        @JsonProperty("@type") String type,
        String name,
        String description,
        List<CuratedListSchemaItemDto> itemListElement
) {
    public static CuratedListItemListSchemaDto from(
            CuratedList list,
            List<CuratedListItemDto> items,
            String siteUrl) {
        String baseUrl = stripTrailingSlash(siteUrl);
        return new CuratedListItemListSchemaDto(
                "https://schema.org",
                "ItemList",
                list.getTitle(),
                list.getDescription(),
                items.stream()
                        .map(item -> CuratedListSchemaItemDto.from(item, baseUrl))
                        .toList());
    }

    private static String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://dondeanime.com";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
