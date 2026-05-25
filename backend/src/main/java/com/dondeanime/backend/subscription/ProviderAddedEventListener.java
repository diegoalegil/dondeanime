package com.dondeanime.backend.subscription;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.dondeanime.backend.provider.ProviderAddedEvent;

@Component
public class ProviderAddedEventListener {

    private static final Logger log = LoggerFactory.getLogger(ProviderAddedEventListener.class);

    private final AlertService alertService;

    public ProviderAddedEventListener(AlertService alertService) {
        this.alertService = alertService;
    }

    @EventListener
    public void onProviderAdded(ProviderAddedEvent event) {
        int sent = alertService.notifyNewProviders(
                event.anime(),
                Map.of(event.countryCode(), event.providers()));
        if (sent > 0) {
            log.info("Alertas enviadas slug={} country={}: {}",
                    event.anime().getSlug(), event.countryCode(), sent);
        }
    }
}
