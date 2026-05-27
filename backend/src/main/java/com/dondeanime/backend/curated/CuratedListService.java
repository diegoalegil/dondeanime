package com.dondeanime.backend.curated;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dondeanime.backend.anime.Anime;
import com.dondeanime.backend.anime.AnimeRepository;
import com.dondeanime.backend.anime.AnimeSummaryDto;

@Service
public class CuratedListService {

    private final CuratedListRepository listRepository;
    private final AnimeRepository animeRepository;
    private final String siteUrl;

    public CuratedListService(
            CuratedListRepository listRepository,
            AnimeRepository animeRepository,
            @Value("${dondeanime.site-url:https://dondeanime.com}") String siteUrl) {
        this.listRepository = listRepository;
        this.animeRepository = animeRepository;
        this.siteUrl = siteUrl;
    }

    @Transactional(readOnly = true)
    public List<CuratedListSummaryDto> publishedLists() {
        return listRepository.findAllByStatusAndVisibilityOrderByTitleAsc(
                        CuratedListStatus.PUBLISHED,
                        CuratedListVisibility.PUBLIC)
                .stream()
                .map(CuratedListSummaryDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<CuratedListDetailDto> publishedList(String slug) {
        return listRepository.findBySlugWithItems(slug)
                .filter(this::isPublishedPublic)
                .map(this::toDetailDto);
    }

    private boolean isPublishedPublic(CuratedList list) {
        return list.getStatus() == CuratedListStatus.PUBLISHED
                && list.getVisibility() == CuratedListVisibility.PUBLIC;
    }

    private CuratedListDetailDto toDetailDto(CuratedList list) {
        List<CuratedListItem> orderedItems = list.orderedItems();
        Set<String> slugs = orderedItems.stream()
                .map(CuratedListItem::getAnimeSlug)
                .collect(Collectors.toSet());
        Map<String, AnimeSummaryDto> animeBySlug = slugs.isEmpty()
                ? Map.of()
                : animeRepository.findBySlugIn(slugs)
                        .stream()
                        .collect(Collectors.toMap(
                                Anime::getSlug,
                                AnimeSummaryDto::from,
                                (first, second) -> first,
                                LinkedHashMap::new));

        List<CuratedListItemDto> items = orderedItems.stream()
                .map(item -> CuratedListItemDto.from(item, animeBySlug.get(item.getAnimeSlug())))
                .filter(item -> item.anime() != null)
                .toList();

        return CuratedListDetailDto.from(list, items, siteUrl);
    }
}
