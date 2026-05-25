package com.dondeanime.backend.premium;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dondeanime.backend.email.EmailService;

@Service
public class PremiumCancellationEmailService {

    private static final Logger log = LoggerFactory.getLogger(PremiumCancellationEmailService.class);
    private static final Duration CANCELLATION_GRACE_PERIOD = Duration.ofDays(30);

    private final SubscriberService subscriberService;
    private final EmailService emailService;
    private final Clock clock;
    private final String premiumUrl;

    @Autowired
    public PremiumCancellationEmailService(
            SubscriberService subscriberService,
            EmailService emailService,
            @Value("${STRIPE_PORTAL_RETURN_URL:https://dondeanime.com/premium}") String premiumUrl) {
        this(subscriberService, emailService, Clock.systemUTC(), premiumUrl);
    }

    PremiumCancellationEmailService(
            SubscriberService subscriberService,
            EmailService emailService,
            Clock clock,
            String premiumUrl) {
        this.subscriberService = subscriberService;
        this.emailService = emailService;
        this.clock = clock;
        this.premiumUrl = premiumUrl;
    }

    public int sendDueCancellationEmails() {
        Instant now = Instant.now(clock);
        Instant cutoff = now.minus(CANCELLATION_GRACE_PERIOD);
        int sent = 0;
        for (Subscriber subscriber : subscriberService.findDueCancellationEmails(cutoff)) {
            try {
                emailService.sendPremiumCancellationEmail(
                        subscriber.getEmail(),
                        subscriber.getPlanTier(),
                        premiumUrl);
                subscriberService.markCancellationEmailSent(subscriber.getId(), now);
                sent++;
            } catch (Exception e) {
                log.error("No se pudo enviar email post-cancelacion premium subscriberId={}: {}",
                        subscriber.getId(), e.getMessage());
            }
        }
        return sent;
    }
}
