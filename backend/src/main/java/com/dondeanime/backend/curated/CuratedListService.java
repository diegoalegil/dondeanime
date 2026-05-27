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

    private static final java.util.regex.Pattern SLUG_PATTERN =
            java.util.regex.Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

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

    @Transactional(readOnly = true)
    public List<CuratedListAdminDto> adminLists() {
        return listRepository.findAll().stream()
                .map(this::toAdminDto)
                .toList();
    }

    @Transactional
    public CuratedListAdminDto saveAdminList(CuratedListSaveRequest request) {
        String slug = normalizedSlug(request.slug(), request.title());
        CuratedList list = listRepository.findBySlugWithItems(slug)
                .orElseGet(CuratedList::new);
        list.setSlug(slug);
        list.setTitle(request.title().trim());
        list.setDescription(request.description().trim());
        list.setOwner(request.owner().trim());
        list.setVisibility(Optional.ofNullable(request.visibility()).orElse(CuratedListVisibility.PRIVATE));
        list.setStatus(Optional.ofNullable(request.status()).orElse(CuratedListStatus.DRAFT));

        return toAdminDto(listRepository.save(list));
    }

    @Transactional
    public Optional<CuratedListAdminDto> addAdminItem(String slug, CuratedListItemSaveRequest request) {
        if (animeRepository.findBySlug(request.animeSlug()).isEmpty()) {
            return Optional.empty();
        }
        return listRepository.findBySlugWithItems(slug)
                .map(list -> {
                    Optional<CuratedListItem> existing = list.getItems().stream()
                            .filter(item -> item.getAnimeSlug().equals(request.animeSlug()))
                            .findFirst();
                    CuratedListItem item = existing.orElseGet(CuratedListItem::new);
                    item.setAnimeSlug(request.animeSlug());
                    item.setNote(blankToNull(request.note()));
                    if (existing.isEmpty()) {
                        item.setPosition(nextPosition(list));
                        list.addItem(item);
                    }
                    return toAdminDto(listRepository.save(list));
                });
    }

    @Transactional
    public Optional<CuratedListAdminDto> moveAdminItem(String slug, String animeSlug, int direction) {
        return listRepository.findBySlugWithItems(slug)
                .flatMap(list -> moveItem(list, animeSlug, direction));
    }

    @Transactional
    public Optional<CuratedListAdminDto> deleteAdminItem(String slug, String animeSlug) {
        return listRepository.findBySlugWithItems(slug)
                .map(list -> {
                    list.getItems().removeIf(item -> item.getAnimeSlug().equals(animeSlug));
                    renumber(list);
                    return toAdminDto(listRepository.save(list));
                });
    }

    @Transactional
    public Optional<CuratedListAdminDto> publishAdminList(String slug) {
        return listRepository.findBySlugWithItems(slug)
                .map(list -> {
                    list.setStatus(CuratedListStatus.PUBLISHED);
                    list.setVisibility(CuratedListVisibility.PUBLIC);
                    return toAdminDto(listRepository.save(list));
                });
    }

    private boolean isPublishedPublic(CuratedList list) {
        return list.getStatus() == CuratedListStatus.PUBLISHED
                && list.getVisibility() == CuratedListVisibility.PUBLIC;
    }

    private CuratedListDetailDto toDetailDto(CuratedList list) {
        List<CuratedListItemDto> items = hydrateItems(list);
        return CuratedListDetailDto.from(list, items, siteUrl);
    }

    private CuratedListAdminDto toAdminDto(CuratedList list) {
        return CuratedListAdminDto.from(list, hydrateItems(list));
    }

    private List<CuratedListItemDto> hydrateItems(CuratedList list) {
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

        return orderedItems.stream()
                .map(item -> CuratedListItemDto.from(item, animeBySlug.get(item.getAnimeSlug())))
                .filter(item -> item.anime() != null)
                .toList();
    }

    private Optional<CuratedListAdminDto> moveItem(CuratedList list, String animeSlug, int direction) {
        List<CuratedListItem> ordered = new java.util.ArrayList<>(list.orderedItems());
        int currentIndex = -1;
        for (int index = 0; index < ordered.size(); index += 1) {
            if (ordered.get(index).getAnimeSlug().equals(animeSlug)) {
                currentIndex = index;
                break;
            }
        }
        if (currentIndex < 0) {
            return Optional.empty();
        }

        int targetIndex = currentIndex + direction;
        if (targetIndex < 0 || targetIndex >= ordered.size()) {
            return Optional.of(toAdminDto(list));
        }

        CuratedListItem current = ordered.get(currentIndex);
        CuratedListItem target = ordered.get(targetIndex);
        int currentPosition = current.getPosition();
        int targetPosition = target.getPosition();
        current.setPosition(nextPosition(list));
        listRepository.flush();
        target.setPosition(currentPosition);
        current.setPosition(targetPosition);

        return Optional.of(toAdminDto(listRepository.save(list)));
    }

    private static void renumber(CuratedList list) {
        List<CuratedListItem> ordered = list.orderedItems();
        for (int index = 0; index < ordered.size(); index += 1) {
            ordered.get(index).setPosition(index + 1);
        }
    }

    private static int nextPosition(CuratedList list) {
        return list.getItems().stream()
                .mapToInt(CuratedListItem::getPosition)
                .max()
                .orElse(0) + 1;
    }

    private String normalizedSlug(String requestedSlug, String title) {
        String slug = requestedSlug == null || requestedSlug.isBlank()
                ? uniqueSlug(CuratedList.slugify(title))
                : requestedSlug.trim().toLowerCase();
        if (!SLUG_PATTERN.matcher(slug).matches()) {
            throw new IllegalArgumentException("slug no valido");
        }
        return slug;
    }

    private String uniqueSlug(String base) {
        String slug = base.isBlank() ? "lista" : base;
        String candidate = slug;
        int suffix = 2;
        while (listRepository.existsBySlug(candidate)) {
            candidate = slug + "-" + suffix;
            suffix += 1;
        }
        return candidate;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
