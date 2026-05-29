package com.dondeanime.backend.curated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class CuratedListPermissionServiceTest {

    private final CuratorProfileRepository curatorRepository = mock(CuratorProfileRepository.class);
    private final CuratedListPermissionService service = new CuratedListPermissionService(curatorRepository);

    @Test
    void adminCanEditAnyDraft() {
        CuratedList list = draftOwnedBy("owner@example.com");

        assertThat(service.canEditDraft(list, "other@example.com", true)).isTrue();
    }

    @Test
    void approvedOwnerCanEditDraft() {
        CuratedList list = draftOwnedBy("owner@example.com");
        when(curatorRepository.existsByEmailIgnoreCaseAndApprovedTrue("owner@example.com"))
                .thenReturn(true);

        assertThat(service.canEditDraft(list, "OWNER@example.com", false)).isTrue();
    }

    @Test
    void otherCuratorCannotEditDraft() {
        CuratedList list = draftOwnedBy("owner@example.com");
        when(curatorRepository.existsByEmailIgnoreCaseAndApprovedTrue("other@example.com"))
                .thenReturn(true);

        assertThat(service.canEditDraft(list, "other@example.com", false)).isFalse();
    }

    @Test
    void revokedOwnerCannotEditDraft() {
        CuratedList list = draftOwnedBy("owner@example.com");
        when(curatorRepository.existsByEmailIgnoreCaseAndApprovedTrue("owner@example.com"))
                .thenReturn(false);

        assertThat(service.canEditDraft(list, "owner@example.com", false)).isFalse();
    }

    @Test
    void ownerCannotEditPublishedListThroughDraftPermission() {
        CuratedList list = draftOwnedBy("owner@example.com");
        list.setStatus(CuratedListStatus.PUBLISHED);
        when(curatorRepository.existsByEmailIgnoreCaseAndApprovedTrue("owner@example.com"))
                .thenReturn(true);

        assertThat(service.canEditDraft(list, "owner@example.com", false)).isFalse();
    }

    private static CuratedList draftOwnedBy(String owner) {
        CuratedList list = new CuratedList();
        list.setSlug("anime-para-empezar");
        list.setTitle("Anime para empezar");
        list.setDescription("Lista curada");
        list.setOwner(owner);
        list.setStatus(CuratedListStatus.DRAFT);
        list.setVisibility(CuratedListVisibility.PRIVATE);
        return list;
    }
}
