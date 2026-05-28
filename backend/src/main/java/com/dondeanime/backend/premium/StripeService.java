package com.dondeanime.backend.premium;

import java.time.Instant;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.dondeanime.backend.email.EmailService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;

@Service
public class StripeService {

    private final StripeGateway stripeGateway;
    private final SubscriberService subscriberService;
    private final EmailService emailService;
    private final StripeProcessedEventRepository processedEventRepository;
    private final String apiKey;
    private final String priceId;
    private final String webhookSecret;
    private final String successUrl;
    private final String cancelUrl;
    private final String portalReturnUrl;

    public StripeService(
            StripeGateway stripeGateway,
            SubscriberService subscriberService,
            EmailService emailService,
            StripeProcessedEventRepository processedEventRepository,
            @Value("${STRIPE_SECRET_KEY:}") String apiKey,
            @Value("${STRIPE_PRICE_ID:}") String priceId,
            @Value("${STRIPE_WEBHOOK_SECRET:}") String webhookSecret,
            @Value("${STRIPE_SUCCESS_URL:https://dondeanime.com/premium?success=1}") String successUrl,
            @Value("${STRIPE_CANCEL_URL:https://dondeanime.com/premium?canceled=1}") String cancelUrl,
            @Value("${STRIPE_PORTAL_RETURN_URL:https://dondeanime.com/premium}") String portalReturnUrl) {
        this.stripeGateway = stripeGateway;
        this.subscriberService = subscriberService;
        this.emailService = emailService;
        this.processedEventRepository = processedEventRepository;
        this.apiKey = apiKey;
        this.priceId = priceId;
        this.webhookSecret = webhookSecret;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
        this.portalReturnUrl = portalReturnUrl;
    }

    public String createCheckoutSession(String email) {
        String normalizedEmail = SubscriberService.normalizeEmail(email);
        if (normalizedEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email requerido");
        }
        assertCheckoutConfigured();
        try {
            return stripeGateway.createCheckoutSession(new StripeCheckoutCommand(
                    apiKey,
                    priceId,
                    normalizedEmail,
                    successUrl,
                    cancelUrl));
        } catch (StripeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe no pudo crear Checkout", e);
        }
    }

    public StripeWebhookEvent verifyWebhookSignature(String payload, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe webhook no configurado");
        }
        try {
            return stripeGateway.constructWebhookEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Firma Stripe invalida", e);
        }
    }

    /**
     * Pide el enlace al portal de cliente de Stripe. Por seguridad:
     *  - NO devuelve la URL al llamante (evita IDOR: cualquiera con un email
     *    ajeno conseguiría gestionar su facturación). El enlace se envía por
     *    email al titular del buzón, que es la prueba de posesión.
     *  - Respuesta genérica tanto si el email es suscriptor como si no
     *    (evita enumeración de suscriptores).
     */
    public void requestCustomerPortalLink(String email) {
        String normalizedEmail = SubscriberService.normalizeEmail(email);
        if (normalizedEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email requerido");
        }
        assertStripeConfigured();
        Optional<String> customerId = subscriberService.findActiveStripeCustomerId(normalizedEmail);
        if (customerId.isEmpty()) {
            return;
        }
        String portalUrl;
        try {
            portalUrl = stripeGateway.createCustomerPortalSession(new StripePortalCommand(
                    apiKey,
                    customerId.get(),
                    portalReturnUrl));
        } catch (StripeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe no pudo crear Customer Portal", e);
        }
        emailService.sendPremiumPortalEmail(normalizedEmail, "PREMIUM", portalUrl);
    }

    @Transactional
    public String handleWebhook(String payload, String signature) {
        StripeWebhookEvent event = verifyWebhookSignature(payload, signature);
        String eventId = event.eventId();
        // Idempotencia: Stripe reentrega eventos. Si ya lo procesamos, no
        // volvemos a tocar BD ni a reenviar emails.
        if (eventId != null && !eventId.isBlank() && processedEventRepository.existsById(eventId)) {
            return event.type();
        }
        Instant eventTime = event.eventTime() == null ? Instant.now() : event.eventTime();
        switch (event.type()) {
            case "customer.subscription.created" -> {
                subscriberService.upsertPremium(
                        event.email(),
                        event.customerId(),
                        "PREMIUM",
                        eventTime,
                        event.currentPeriodEnd(),
                        null);
                sendWelcomeEmail(event);
            }
            case "customer.subscription.updated" -> subscriberService.upsertPremium(
                        event.email(),
                        event.customerId(),
                        "PREMIUM",
                        eventTime,
                        event.currentPeriodEnd(),
                        null);
            case "customer.subscription.deleted" -> subscriberService.cancelByStripeCustomerId(
                        event.customerId(),
                        eventTime);
            case "invoice.payment_succeeded" -> {
                subscriberService.recordPaymentSucceeded(
                        event.email(),
                        event.customerId(),
                        eventTime);
                sendReceiptEmail(event, eventTime);
            }
            case "invoice.payment_failed" -> {
                // Stripe retries failed invoices; access is revoked by subscription.deleted if it never recovers.
            }
            default -> {
            }
        }
        if (eventId != null && !eventId.isBlank()) {
            processedEventRepository.save(new StripeProcessedEvent(eventId, Instant.now()));
        }
        return event.type();
    }

    private void sendWelcomeEmail(StripeWebhookEvent event) {
        String email = resolveEmail(event);
        if (!email.isBlank()) {
            emailService.sendPremiumWelcomeEmail(email, "PREMIUM", portalReturnUrl);
        }
    }

    private void sendReceiptEmail(StripeWebhookEvent event, Instant paidAt) {
        String email = resolveEmail(event);
        if (!email.isBlank()) {
            emailService.sendPremiumReceiptEmail(email, "PREMIUM", paidAt.toString(), portalReturnUrl);
        }
    }

    private String resolveEmail(StripeWebhookEvent event) {
        String email = SubscriberService.normalizeEmail(event.email());
        if (!email.isBlank()) {
            return email;
        }
        return subscriberService.findEmailByStripeCustomerId(event.customerId()).orElse("");
    }

    private void assertCheckoutConfigured() {
        assertStripeConfigured();
        if (apiKey == null || apiKey.isBlank() || priceId == null || priceId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe Checkout no configurado");
        }
    }

    private void assertStripeConfigured() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe no configurado");
        }
    }
}
