package com.dondeanime.backend.affiliate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AffiliateClickEventRepository extends JpaRepository<AffiliateClickEvent, Long> {

    long countByClickedAtAfter(Instant since);

    @Query(value = """
            SELECT CAST(clicked_at AS date) AS "clickDate", COUNT(*) AS clicks
              FROM affiliate_click_event
             WHERE clicked_at >= :since
             GROUP BY CAST(clicked_at AS date)
             ORDER BY CAST(clicked_at AS date)
            """, nativeQuery = true)
    List<DailyClickProjection> countClicksByDay(Instant since);

    @Query("""
            SELECT e.animeSlug AS animeSlug, COUNT(e) AS clicks
              FROM AffiliateClickEvent e
             WHERE e.clickedAt >= :since
             GROUP BY e.animeSlug
             ORDER BY COUNT(e) DESC, e.animeSlug ASC
            """)
    List<AnimeClickProjection> findTopAnimeClicks(Instant since, Pageable pageable);

    @Query("""
            SELECT e.providerSlug AS providerSlug, COUNT(e) AS clicks
              FROM AffiliateClickEvent e
             WHERE e.clickedAt >= :since
             GROUP BY e.providerSlug
             ORDER BY COUNT(e) DESC, e.providerSlug ASC
            """)
    List<ProviderClickProjection> findTopProviderClicks(Instant since, Pageable pageable);

    @Query("""
            SELECT e.countryCode AS countryCode, COUNT(e) AS clicks
              FROM AffiliateClickEvent e
             WHERE e.clickedAt >= :since
             GROUP BY e.countryCode
             ORDER BY COUNT(e) DESC, e.countryCode ASC
            """)
    List<CountryClickProjection> findTopCountryClicks(Instant since, Pageable pageable);

    interface DailyClickProjection {
        LocalDate getClickDate();
        Long getClicks();
    }

    interface AnimeClickProjection {
        String getAnimeSlug();
        Long getClicks();
    }

    interface ProviderClickProjection {
        String getProviderSlug();
        Long getClicks();
    }

    interface CountryClickProjection {
        String getCountryCode();
        Long getClicks();
    }
}
