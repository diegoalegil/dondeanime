package com.dondeanime.backend.push;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MobilePushDeviceRepository extends JpaRepository<MobilePushDevice, Long> {

    Optional<MobilePushDevice> findByDeviceToken(String deviceToken);
}
