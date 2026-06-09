package com.dondeanime.backend.premium;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.dondeanime.backend.email.EmailService;

/**
 * Envía los emails de Premium DESPUÉS del commit del webhook de Stripe.
 * El email es recuperable (reintento, soporte); el cobro registrado no:
 * por eso un fallo de envío solo se loguea y nunca se propaga.
 */
@Component
public class PremiumEmailEventListener {

    private static final Logger log = LoggerFactory.getLogger(PremiumEmailEventListener.class);

    private final EmailService emailService;

    public PremiumEmailEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPremiumEmail(PremiumEmailEvent event) {
        try {
            switch (event.type()) {
                case WELCOME -> emailService.sendPremiumWelcomeEmail(
                        event.email(), event.planTier(), event.manageUrl());
                case RECEIPT -> emailService.sendPremiumReceiptEmail(
                        event.email(), event.planTier(), event.paidAt(), event.manageUrl());
            }
        } catch (RuntimeException e) {
            log.error("Error enviando email premium {} a '{}': {}",
                    event.type(), event.email(), e.getMessage());
        }
    }
}
