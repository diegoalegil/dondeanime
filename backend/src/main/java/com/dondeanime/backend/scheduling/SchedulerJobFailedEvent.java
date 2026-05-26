package com.dondeanime.backend.scheduling;

import java.util.Objects;

public record SchedulerJobFailedEvent(String job, Throwable error) {

    public SchedulerJobFailedEvent {
        Objects.requireNonNull(job, "job no puede ser null");
        Objects.requireNonNull(error, "error no puede ser null");
        if (job.isBlank()) {
            throw new IllegalArgumentException("job no puede estar vacío");
        }
    }
}
