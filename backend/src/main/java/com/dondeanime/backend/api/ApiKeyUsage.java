package com.dondeanime.backend.api;

public record ApiKeyUsage(
        String tier,
        long monthlyQuota,
        long monthlyUsage,
        long remaining
) {}
