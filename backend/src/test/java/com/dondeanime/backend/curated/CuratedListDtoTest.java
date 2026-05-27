package com.dondeanime.backend.curated;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CuratedListDtoTest {

    @Test
    void detailDtoOrdersItemsAndHidesEmailOwner() {
        CuratedList list = list("curator@example.com");
        list.addItem(item("naruto", 2, "Shonen largo."));
        list.addItem(item("frieren-beyond-journeys-end", 1, "Fantasia moderna."));

        CuratedListDetailDto dto = CuratedListDetailDto.from(list);

        assertThat(dto.slug()).isEqualTo("anime-para-empezar");
        assertThat(dto.owner()).isNull();
        assertThat(dto.items())
                .extracting(CuratedListItemDto::animeSlug)
                .containsExactly("frieren-beyond-journeys-end", "naruto");
        assertThat(dto.schema().type()).isEqualTo("ItemList");
        assertThat(dto.schema().itemListElement())
                .extracting(CuratedListSchemaItemDto::url)
                .containsExactly(
                        "https://dondeanime.com/anime/frieren-beyond-journeys-end",
                        "https://dondeanime.com/anime/naruto");
    }

    @Test
    void summaryDtoIncludesPublicOwnerAndCountWithoutInternalIds() {
        CuratedList list = list("Diego");
        list.setId(99L);
        list.addItem(item("one-piece", 1, null));
        list.addItem(item("bleach", 2, null));

        CuratedListSummaryDto dto = CuratedListSummaryDto.from(list);

        assertThat(dto.slug()).isEqualTo("anime-para-empezar");
        assertThat(dto.owner()).isEqualTo("Diego");
        assertThat(dto.status()).isEqualTo("PUBLISHED");
        assertThat(dto.visibility()).isEqualTo("PUBLIC");
        assertThat(dto.itemCount()).isEqualTo(2);
    }

    private static CuratedList list(String owner) {
        CuratedList list = new CuratedList();
        list.setSlug("anime-para-empezar");
        list.setTitle("Anime para empezar");
        list.setDescription("Lista curada para nuevos lectores.");
        list.setOwner(owner);
        list.setVisibility(CuratedListVisibility.PUBLIC);
        list.setStatus(CuratedListStatus.PUBLISHED);
        return list;
    }

    private static CuratedListItem item(String animeSlug, int position, String note) {
        CuratedListItem item = new CuratedListItem();
        item.setAnimeSlug(animeSlug);
        item.setPosition(position);
        item.setNote(note);
        return item;
    }
}
