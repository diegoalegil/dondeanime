package com.dondeanime.backend.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true")
public class PushSubscriptionCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(PushSubscriptionCleanupScheduler.class);

    private final PushSubscriptionCleanupService cleanupService;

    public PushSubscriptionCleanupScheduler(PushSubscriptionCleanupService cleanupService) {
        this.cleanupService = cleanupService;
    }

    @Scheduled(cron = "${dondeanime.cron.cleanup-push-subscriptions:0 30 2 * * *}")
    public void purgeInactivePushSubscriptions() {
        log.info("[scheduler] purgeInactivePushSubscriptions: iniciando");
        try {
            int purged = cleanupService.purgeInactiveSubscriptions();
            log.info("[scheduler] purgeInactivePushSubscriptions: {} subscriptions eliminadas", purged);
        } catch (Exception e) {
            log.error("[scheduler] purgeInactivePushSubscriptions: ERROR", e);
        }
    }
}
