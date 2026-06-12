package com.dondeanime.backend.admin.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AdminTokenRevocationServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-12T12:00:00Z");

    private final AdminRevokedTokenRepository repository = mock(AdminRevokedTokenRepository.class);
    private final AdminTokenRevocationService service =
            new AdminTokenRevocationService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void revokeStoresJtiUntilExpirationAndPurgesExpired() {
        AdminTokenClaims claims = new AdminTokenClaims("jti-1", NOW.plusSeconds(3600));

        service.revoke(claims);

        verify(repository).deleteByExpiresAtBefore(NOW);
        ArgumentCaptor<AdminRevokedToken> captor = ArgumentCaptor.forClass(AdminRevokedToken.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getJti()).isEqualTo("jti-1");
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(NOW.plusSeconds(3600));
        assertThat(captor.getValue().getRevokedAt()).isEqualTo(NOW);
    }

    @Test
    void revokeIsIdempotent() {
        when(repository.existsById("jti-1")).thenReturn(true);

        service.revoke(new AdminTokenClaims("jti-1", NOW.plusSeconds(3600)));

        verify(repository, never()).save(any());
    }

    @Test
    void isRevokedDelegatesToRepository() {
        when(repository.existsById("jti-1")).thenReturn(true);

        assertThat(service.isRevoked("jti-1")).isTrue();
        assertThat(service.isRevoked("jti-2")).isFalse();
    }
}
