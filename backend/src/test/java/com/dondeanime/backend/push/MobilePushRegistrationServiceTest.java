package com.dondeanime.backend.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MobilePushRegistrationServiceTest {

    private final MobilePushDeviceRepository repository = mock(MobilePushDeviceRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-27T12:00:00Z"), ZoneOffset.UTC);
    private final MobilePushRegistrationService service = new MobilePushRegistrationService(repository, clock);

    @Test
    void registersNewDeviceForRequestedAlertsOnly() {
        when(repository.findByDeviceToken("token-123")).thenReturn(Optional.empty());
        when(repository.save(any(MobilePushDevice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MobilePushRegistrationResponse response = service.register(new MobilePushRegistrationRequest(
                "ios",
                " token-123 ",
                "es"));

        ArgumentCaptor<MobilePushDevice> captor = ArgumentCaptor.forClass(MobilePushDevice.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        MobilePushDevice saved = captor.getValue();
        assertThat(saved.getPlatform()).isEqualTo("IOS");
        assertThat(saved.getDeviceToken()).isEqualTo("token-123");
        assertThat(saved.getCountryIso()).isEqualTo("ES");
        assertThat(saved.getAlertsOnly()).isTrue();
        assertThat(saved.getEnabled()).isTrue();
        assertThat(saved.getCreatedAt()).isEqualTo("2026-05-27T12:00:00Z");
        assertThat(response.alertsOnly()).isTrue();
    }

    @Test
    void updatesExistingDeviceCountryAndPlatform() {
        MobilePushDevice existing = new MobilePushDevice();
        existing.setDeviceToken("token-123");
        existing.setCreatedAt(Instant.parse("2026-05-26T12:00:00Z"));
        when(repository.findByDeviceToken("token-123")).thenReturn(Optional.of(existing));
        when(repository.save(any(MobilePushDevice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MobilePushRegistrationResponse response = service.register(new MobilePushRegistrationRequest(
                "android",
                "token-123",
                "mx"));

        assertThat(response.platform()).isEqualTo("ANDROID");
        assertThat(existing.getCountryIso()).isEqualTo("MX");
        assertThat(existing.getCreatedAt()).isEqualTo("2026-05-26T12:00:00Z");
        assertThat(existing.getUpdatedAt()).isEqualTo("2026-05-27T12:00:00Z");
    }
}
