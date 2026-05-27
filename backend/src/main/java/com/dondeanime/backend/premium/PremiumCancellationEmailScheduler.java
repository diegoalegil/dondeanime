package com.dondeanime.backend.premium;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true")
public class PremiumCancellationEmailScheduler {

    private static final Logger log = LoggerFactory.getLogger(PremiumCancellationEmailScheduler.class);

    private final PremiumCancellationEmailService premiumCancellationEmailService;

    public PremiumCancellationEmailScheduler(PremiumCancellationEmailService premiumCancellationEmailService) {
        this.premiumCancellationEmailService = premiumCancellationEmailService;
    }

    @Scheduled(cron = "${dondeanime.cron.premium-cancellation-email:0 45 3 * * *}")
    public void sendPremiumCancellationEmails() {
        log.info("[scheduler] sendPremiumCancellationEmails: iniciando");
        try {
            int sent = premiumCancellationEmailService.sendDueCancellationEmails();
            log.info("[scheduler] sendPremiumCancellationEmails: {} emails enviados", sent);
        } catch (Exception e) {
            log.error("[scheduler] sendPremiumCancellationEmails: ERROR", e);
        }
    }
}
