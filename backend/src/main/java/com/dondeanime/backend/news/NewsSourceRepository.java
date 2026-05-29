package com.dondeanime.backend.news;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsSourceRepository extends JpaRepository<NewsSource, Long> {

    List<NewsSource> findByEnabledTrue();

    Optional<NewsSource> findByName(String name);

    boolean existsByName(String name);
}
