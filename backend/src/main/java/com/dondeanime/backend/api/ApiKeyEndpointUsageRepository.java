package com.dondeanime.backend.api;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ApiKeyEndpointUsageRepository extends JpaRepository<ApiKeyEndpointUsage, Long> {

    Optional<ApiKeyEndpointUsage> findByApiKey_IdAndEndpoint(Long apiKeyId, String endpoint);

    @Modifying
    @Query("update ApiKeyEndpointUsage u set u.monthlyUsage = 0 where u.apiKey.id = :apiKeyId")
    void resetMonthlyUsageByApiKeyId(Long apiKeyId);

    @Query("""
            select u.endpoint as endpoint, sum(u.monthlyUsage) as monthlyUsage
            from ApiKeyEndpointUsage u
            where u.monthlyUsage > 0
            group by u.endpoint
            order by sum(u.monthlyUsage) desc
            """)
    List<EndpointUsageTotal> findTopEndpoints(Pageable pageable);

    interface EndpointUsageTotal {
        String getEndpoint();

        long getMonthlyUsage();
    }
}
