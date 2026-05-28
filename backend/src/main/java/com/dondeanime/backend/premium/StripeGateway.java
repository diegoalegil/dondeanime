package com.dondeanime.backend.premium;

import java.time.Instant;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;

@Component
public class StripeGateway {

    public String createCheckoutSession(StripeCheckoutCommand command) throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomerEmail(command.email())
                .setSuccessUrl(command.successUrl())
                .setCancelUrl(command.cancelUrl())
                .putMetadata("email", command.email())
                .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                        .putMetadata("email", command.email())
                        .putMetadata("planTier", "PREMIUM")
                        .build())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(command.priceId())
                        .setQuantity(1L)
                        .build())
                .build();
        RequestOptions options = RequestOptions.builder()
                .setApiKey(command.apiKey())
                .build();
        return Session.create(params, options).getUrl();
    }

    public String createCustomerPortalSession(StripePortalCommand command) throws StripeException {
        com.stripe.param.billingportal.SessionCreateParams params =
                com.stripe.param.billingportal.SessionCreateParams.builder()
                        .setCustomer(command.customerId())
                        .setReturnUrl(command.returnUrl())
                        .build();
        RequestOptions options = RequestOptions.builder()
                .setApiKey(command.apiKey())
                .build();
        return com.stripe.model.billingportal.Session.create(params, options).getUrl();
    }

    public StripeWebhookEvent constructWebhookEvent(
            String payload,
            String signature,
            String webhookSecret) throws SignatureVerificationException {
        Event event = Webhook.constructEvent(payload, signature, webhookSecret);
        StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
        return toWebhookEvent(event, object);
    }

    private static StripeWebhookEvent toWebhookEvent(Event event, StripeObject object) {
        if (object instanceof com.stripe.model.Subscription subscription) {
            Map<String, String> metadata = subscription.getMetadata();
            String email = metadata == null ? null : metadata.get("email");
            return new StripeWebhookEvent(
                    event.getType(),
                    email,
                    subscription.getCustomer(),
                    toInstant(event.getCreated()),
                    toInstant(subscription.getCurrentPeriodEnd()),
                    event.getId());
        }
        if (object instanceof Invoice invoice) {
            return new StripeWebhookEvent(
                    event.getType(),
                    invoice.getCustomerEmail(),
                    invoice.getCustomer(),
                    toInstant(event.getCreated()),
                    null,
                    event.getId());
        }
        return new StripeWebhookEvent(
                event.getType(), null, null, toInstant(event.getCreated()), null, event.getId());
    }

    private static Instant toInstant(Long epochSeconds) {
        return epochSeconds == null ? null : Instant.ofEpochSecond(epochSeconds);
    }
}
