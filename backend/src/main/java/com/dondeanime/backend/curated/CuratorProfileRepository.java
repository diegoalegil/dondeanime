package com.dondeanime.backend.curated;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CuratorProfileRepository extends JpaRepository<CuratorProfile, Long> {

    Optional<CuratorProfile> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndApprovedTrue(String email);
}
