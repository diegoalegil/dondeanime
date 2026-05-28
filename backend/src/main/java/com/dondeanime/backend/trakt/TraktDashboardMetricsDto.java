package com.dondeanime.backend.trakt;

public record TraktDashboardMetricsDto(
        Long connectedAccounts,
        Long syncsLast7Days,
        Long syncsLast30Days,
        Long failedMatchesLast30Days) {
}
