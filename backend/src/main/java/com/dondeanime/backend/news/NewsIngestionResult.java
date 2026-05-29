package com.dondeanime.backend.news;

import java.util.List;

/** Resumen de una pasada de ingesta, para logs y para el endpoint admin. */
public record NewsIngestionResult(
        int sourcesProcessed,
        int itemsCreated,
        int itemsSkipped,
        int itemsErrored,
        List<SourceResult> sources) {

    /** Desglose por fuente: cuántas trajo y cuántas se crearon/saltaron/fallaron. */
    public record SourceResult(
            String source,
            int fetched,
            int created,
            int skipped,
            int errors,
            boolean ok) {
    }
}
