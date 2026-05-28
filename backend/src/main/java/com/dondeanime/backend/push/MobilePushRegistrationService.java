package com.dondeanime.backend.push;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dondeanime.backend.subscription.CountryCatalog;

@Service
public class MobilePushRegistrationService {

    private final MobilePushDeviceRepository repository;
    private final Clock clock;

    public MobilePushRegistrationService(MobilePushDeviceRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public MobilePushRegistrationResponse register(MobilePushRegistrationRequest request) {
        String platform = request.platform().trim().toUpperCase(Locale.ROOT);
        String deviceToken = request.deviceToken().trim();
        String countryIso = CountryCatalog.normalizeCountry(request.countryIso());
        Instant now = Instant.now(clock);

        MobilePushDevice device = repository.findByDeviceToken(deviceToken)
                .orElseGet(() -> {
                    MobilePushDevice created = new MobilePushDevice();
                    created.setDeviceToken(deviceToken);
                    created.setCreatedAt(now);
                    return created;
                });

        device.setPlatform(platform);
        device.setCountryIso(countryIso);
        device.setAlertsOnly(true);
        device.setEnabled(true);
        device.setUpdatedAt(now);
        repository.save(device);

        return new MobilePushRegistrationResponse(
                platform,
                countryIso,
                true,
                "Dispositivo movil registrado para alertas solicitadas.");
    }
}
