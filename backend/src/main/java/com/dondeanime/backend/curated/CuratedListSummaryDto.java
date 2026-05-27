package com.dondeanime.backend.curated;

public record CuratedListSummaryDto(
        String slug,
        String title,
        String description,
        String owner,
        String visibility,
        String status,
        int itemCount
) {
    public static CuratedListSummaryDto from(CuratedList list) {
        return new CuratedListSummaryDto(
                list.getSlug(),
                list.getTitle(),
                list.getDescription(),
                publicOwner(list.getOwner()),
                list.getVisibility().name(),
                list.getStatus().name(),
                list.getItems().size());
    }

    static String publicOwner(String owner) {
        if (owner == null || owner.contains("@")) {
            return null;
        }
        return owner;
    }
}
