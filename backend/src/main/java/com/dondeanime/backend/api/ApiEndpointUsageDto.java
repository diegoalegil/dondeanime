package com.dondeanime.backend.api;

public record ApiEndpointUsageDto(
        String endpoint,
        long monthlyUsage) {

    static ApiEndpointUsageDto from(ApiKeyEndpointUsageRepository.EndpointUsageTotal usage) {
        return new ApiEndpointUsageDto(usage.getEndpoint(), usage.getMonthlyUsage());
    }
}
