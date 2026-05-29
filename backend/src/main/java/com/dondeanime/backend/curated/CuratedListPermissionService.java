package com.dondeanime.backend.curated;

import org.springframework.stereotype.Service;

@Service
public class CuratedListPermissionService {

    private final CuratorProfileRepository curatorRepository;

    public CuratedListPermissionService(CuratorProfileRepository curatorRepository) {
        this.curatorRepository = curatorRepository;
    }

    public boolean canEditDraft(CuratedList list, String actorEmail, boolean admin) {
        if (admin) {
            return true;
        }
        if (list.getStatus() != CuratedListStatus.DRAFT) {
            return false;
        }
        String normalizedActor = CuratorProfile.normalizeEmail(actorEmail);
        if (normalizedActor.isBlank() || !normalizedActor.equals(CuratorProfile.normalizeEmail(list.getOwner()))) {
            return false;
        }
        return curatorRepository.existsByEmailIgnoreCaseAndApprovedTrue(normalizedActor);
    }
}
