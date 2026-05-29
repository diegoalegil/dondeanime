package com.dondeanime.backend.trakt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class TraktOAuthStateServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-27T10:00:00Z");

    @Test
    void createsSignedStateThatCanBeValidated() {
        TraktOAuthStateService service = new TraktOAuthStateService(
                Clock.fixed(NOW, ZoneOffset.UTC),
                "trakt-secret");

        String state = service.createState();

        assertThat(service.isValid(state)).isTrue();
    }

    @Test
    void rejectsTamperedState() {
        TraktOAuthStateService service = new TraktOAuthStateService(
                Clock.fixed(NOW, ZoneOffset.UTC),
                "trakt-secret");

        String state = service.createState();

        assertThat(service.isValid(state + "x")).isFalse();
    }

    @Test
    void rejectsExpiredState() {
        TraktOAuthStateService issuer = new TraktOAuthStateService(
                Clock.fixed(NOW, ZoneOffset.UTC),
                "trakt-secret");
        String state = issuer.createState();

        TraktOAuthStateService validator = new TraktOAuthStateService(
                Clock.fixed(NOW.plus(Duration.ofMinutes(11)), ZoneOffset.UTC),
                "trakt-secret");

        assertThat(validator.isValid(state)).isFalse();
    }
}
