package com.dondeanime.backend.affiliate;

import java.util.List;

public record PlausibleQueryResponse(
        List<PlausibleQueryResult> results
) {
    public record PlausibleQueryResult(
            List<Number> metrics,
            List<String> dimensions
    ) {}
}
