package com.dondeanime.backend.api;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByKey(String key);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ApiKey a where a.key = :key")
    Optional<ApiKey> findByKeyForUpdate(@Param("key") String key);

    boolean existsByKey(String key);
}
