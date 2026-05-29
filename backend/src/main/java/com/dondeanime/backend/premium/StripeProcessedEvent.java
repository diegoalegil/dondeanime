package com.dondeanime.backend.premium;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Registro de idempotencia para webhooks de Stripe. Stripe puede reentregar
 * el mismo evento; guardamos su id para no procesarlo (ni reenviar emails)
 * dos veces.
 */
@Entity
@Table(name = "stripe_processed_event")
public class StripeProcessedEvent {

    @Id
    @Column(name = "event_id", nullable = false, length = 255)
    private String eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected StripeProcessedEvent() {
    }

    public StripeProcessedEvent(String eventId, Instant processedAt) {
        this.eventId = eventId;
        this.processedAt = processedAt;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
