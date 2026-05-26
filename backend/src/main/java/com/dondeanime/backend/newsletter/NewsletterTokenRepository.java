package com.dondeanime.backend.newsletter;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsletterTokenRepository extends JpaRepository<NewsletterToken, Long> {

    Optional<NewsletterToken> findByTokenHash(String tokenHash);
}
