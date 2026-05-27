package com.dondeanime.backend.subscription;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.dondeanime.backend.provider.ProviderAddedEvent;
import com.dondeanime.backend.push.PushNotificationService;

@Component
public class ProviderAddedEventListener {

    private static final Logger log = LoggerFactory.getLogger(ProviderAddedEventListener.class);

    private final AlertService alertService;
    private final PushNotificationService pushNotificationService;

    public ProviderAddedEventListener(
            AlertService alertService,
            PushNotificationService pushNotificationService) {
        this.alertService = alertService;
        this.pushNotificationService = pushNotificationService;
    }

    @EventListener
    public void onProviderAdded(ProviderAddedEvent event) {
        int pushed = pushNotificationService.notifyNewProviders(
                event.anime(),
                event.countryCode(),
                event.providers());
        int sent = alertService.notifyNewProviders(
                event.anime(),
                Map.of(event.countryCode(), event.providers()));
        if (sent > 0 || pushed > 0) {
            log.info("Alertas procesadas slug={} country={} email={} push={}",
                    event.anime().getSlug(), event.countryCode(), sent, pushed);
        }
    }
}
