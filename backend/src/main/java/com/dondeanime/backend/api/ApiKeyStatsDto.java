package com.dondeanime.backend.api;

import java.util.List;

public record ApiKeyStatsDto(
        List<ApiKeyStatsRowDto> keys,
        List<ApiEndpointUsageDto> topEndpoints) {
}
