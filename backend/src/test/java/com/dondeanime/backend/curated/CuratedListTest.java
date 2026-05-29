package com.dondeanime.backend.curated;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class CuratedListTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void slugifyNormalizesTitle() {
        assertThat(CuratedList.slugify("Romance sin relleno")).isEqualTo("romance-sin-relleno");
        assertThat(CuratedList.slugify("Mechas clásicos!!!")).isEqualTo("mechas-clasicos");
        assertThat(CuratedList.slugify("  Anime   corto  ")).isEqualTo("anime-corto");
    }

    @Test
    void validListAndItemPassBeanValidation() {
        CuratedList list = validList();
        CuratedListItem item = item("frieren-beyond-journeys-end", 1, "Fantasia tranquila para empezar.");
        list.addItem(item);

        assertThat(validator.validate(list)).isEmpty();
        assertThat(validator.validate(item)).isEmpty();
        assertThat(item.getCuratedList()).isSameAs(list);
    }

    @Test
    void invalidSlugAndBlankTitleFailValidation() {
        CuratedList list = validList();
        list.setSlug("Romance Malo");
        list.setTitle(" ");

        Set<ConstraintViolation<CuratedList>> violations = validator.validate(list);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("slug", "title");
    }

    @Test
    void invalidItemPositionAndAnimeSlugFailValidation() {
        CuratedListItem item = item("../admin", 0, "x".repeat(601));
        item.setCuratedList(validList());

        Set<ConstraintViolation<CuratedListItem>> violations = validator.validate(item);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("animeSlug", "position", "note");
    }

    private static CuratedList validList() {
        CuratedList list = new CuratedList();
        list.setSlug("romance-sin-relleno");
        list.setTitle("Romance sin relleno");
        list.setDescription("Animes de romance directos y faciles de empezar.");
        list.setOwner("Diego");
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
