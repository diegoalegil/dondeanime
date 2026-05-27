package com.dondeanime.backend.trakt;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalAccountRepository extends JpaRepository<ExternalAccount, Long> {

    Optional<ExternalAccount> findByProviderAndExternalUserId(String provider, String externalUserId);
}
