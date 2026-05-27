package com.dondeanime.backend.curated;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CuratorProfileService {

    private final CuratorProfileRepository repository;

    public CuratorProfileService(CuratorProfileRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CuratorProfileDto> listCurators() {
        return repository.findAll().stream()
                .map(CuratorProfileDto::from)
                .toList();
    }

    @Transactional
    public CuratorProfileDto approveCurator(CuratorProfileSaveRequest request) {
        String email = CuratorProfile.normalizeEmail(request.email());
        CuratorProfile profile = repository.findByEmailIgnoreCase(email)
                .orElseGet(CuratorProfile::new);
        profile.approve(email, request.displayName());
        return CuratorProfileDto.from(repository.save(profile));
    }

    @Transactional
    public Optional<CuratorProfileDto> revokeCurator(String email) {
        return repository.findByEmailIgnoreCase(CuratorProfile.normalizeEmail(email))
                .map(profile -> {
                    profile.revoke();
                    return CuratorProfileDto.from(repository.save(profile));
                });
    }
}
