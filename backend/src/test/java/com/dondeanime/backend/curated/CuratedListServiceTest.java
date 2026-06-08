package com.dondeanime.backend.curated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;

class CuratedListServiceTest {

    private final CuratedListRepository listRepository = mock(CuratedListRepository.class);
    private final AnimeRepository animeRepository = mock(AnimeRepository.class);
    private final CuratedListService service = new CuratedListService(
            listRepository,
            animeRepository,
            "https://dondeanime.com/");

    @Test
    void publishedListsUsesOnlyPublicPublishedLists() {
        CuratedList list = list(CuratedListStatus.PUBLISHED, CuratedListVisibility.PUBLIC);
        list.addItem(item("frieren-beyond-journeys-end", 1));
        when(listRepository.findAllByStatusAndVisibilityOrderByTitleAsc(
                CuratedListStatus.PUBLISHED,
                CuratedListVisibility.PUBLIC))
                .thenReturn(List.of(list));

        List<CuratedListSummaryDto> result = service.publishedLists();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().slug()).isEqualTo("anime-para-empezar");
        assertThat(result.getFirst().itemCount()).isEqualTo(1);
    }

    @Test
    void detailReturnsOrderedExistingAnimeAndSchema() {
        CuratedList list = list(CuratedListStatus.PUBLISHED, CuratedListVisibility.PUBLIC);
        list.addItem(item("naruto", 2));
        list.addItem(item("missing-anime", 3));
        list.addItem(item("frieren-beyond-journeys-end", 1));
        when(listRepository.findBySlugWithItems("anime-para-empezar")).thenReturn(Optional.of(list));
        when(animeRepository.findBySlugIn(Set.of("naruto", "missing-anime", "frieren-beyond-journeys-end")))
                .thenReturn(List.of(
                        anime("frieren-beyond-journeys-end", "Frieren"),
                        anime("naruto", "Naruto")));

        Optional<CuratedListDetailDto> result = service.publishedList("anime-para-empezar");

        assertThat(result).isPresent();
        assertThat(result.get().items())
                .extracting(CuratedListItemDto::animeSlug)
                .containsExactly("frieren-beyond-journeys-end", "naruto");
        assertThat(result.get().items().getFirst().anime().titleEnglish()).isEqualTo("Frieren");
        assertThat(result.get().schema().itemListElement().getFirst().name()).isEqualTo("Frieren");
        assertThat(result.get().schema().itemListElement().getFirst().url())
                .isEqualTo("https://dondeanime.com/anime/frieren-beyond-journeys-end");
    }

    @Test
    void detailHidesDraftOrPrivateLists() {
        CuratedList draft = list(CuratedListStatus.DRAFT, CuratedListVisibility.PUBLIC);
        when(listRepository.findBySlugWithItems("anime-para-empezar")).thenReturn(Optional.of(draft));

        assertThat(service.publishedList("anime-para-empezar")).isEmpty();
        verify(listRepository).findBySlugWithItems("anime-para-empezar");
    }

    @Test
    void premiumOnlyListReturnsPreviewWithoutVerifiedAccess() {
        CuratedList list = list(CuratedListStatus.PUBLISHED, CuratedListVisibility.PUBLIC);
        list.setPremiumOnly(true);
        list.addItem(item("one", 1));
        list.addItem(item("two", 2));
        list.addItem(item("three", 3));
        list.addItem(item("four", 4));
        when(listRepository.findBySlugWithItems("anime-para-empezar")).thenReturn(Optional.of(list));
        when(animeRepository.findBySlugIn(Set.of("one", "two", "three", "four")))
                .thenReturn(List.of(
                        anime("one", "One"),
                        anime("two", "Two"),
                        anime("three", "Three"),
                        anime("four", "Four")));

        Optional<CuratedListDetailDto> result = service.publishedList("anime-para-empezar");

        assertThat(result).isPresent();
        assertThat(result.get().premiumOnly()).isTrue();
        assertThat(result.get().premiumPreview()).isTrue();
        assertThat(result.get().premiumCtaUrl()).isEqualTo("https://dondeanime.com/premium");
        assertThat(result.get().items()).hasSize(3);
    }

    @Test
    void premiumOnlyListReturnsFullListForVerifiedPremiumViewer() {
        CuratedList list = list(CuratedListStatus.PUBLISHED, CuratedListVisibility.PUBLIC);
        list.setPremiumOnly(true);
        list.addItem(item("one", 1));
        list.addItem(item("two", 2));
        list.addItem(item("three", 3));
        list.addItem(item("four", 4));
        when(listRepository.findBySlugWithItems("anime-para-empezar")).thenReturn(Optional.of(list));
        when(animeRepository.findBySlugIn(Set.of("one", "two", "three", "four")))
                .thenReturn(List.of(
                        anime("one", "One"),
                        anime("two", "Two"),
                        anime("three", "Three"),
                        anime("four", "Four")));

        Optional<CuratedListDetailDto> result = service.publishedList("anime-para-empezar", true);

        assertThat(result).isPresent();
        assertThat(result.get().premiumPreview()).isFalse();
        assertThat(result.get().premiumCtaUrl()).isNull();
        assertThat(result.get().items()).hasSize(4);
    }

    private static CuratedList list(CuratedListStatus status, CuratedListVisibility visibility) {
        CuratedList list = new CuratedList();
        list.setSlug("anime-para-empezar");
        list.setTitle("Anime para empezar");
        list.setDescription("Lista curada para nuevos lectores.");
        list.setOwner("Diego");
        list.setVisibility(visibility);
        list.setStatus(status);
        return list;
    }

    private static CuratedListItem item(String animeSlug, int position) {
        CuratedListItem item = new CuratedListItem();
        item.setAnimeSlug(animeSlug);
        item.setPosition(position);
        item.setNote("Nota " + position);
        return item;
    }

    private static Anime anime(String slug, String title) {
        Anime anime = new Anime();
        anime.setAnilistId((long) title.hashCode());
        anime.setSlug(slug);
        anime.setTitleEnglish(title);
        anime.setTitleRomaji(title);
        anime.setFormat("TV");
        anime.setStatus("FINISHED");
        return anime;
    }
}
