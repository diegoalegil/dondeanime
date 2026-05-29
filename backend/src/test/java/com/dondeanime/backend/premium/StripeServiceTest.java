package com.dondeanime.backend.premium;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.dondeanime.backend.curated.CuratedListTrackingService;
import com.dondeanime.backend.email.EmailService;
import com.stripe.exception.SignatureVerificationException;

class StripeServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-25T10:00:00Z");

    private final StripeGateway stripeGateway = mock(StripeGateway.class);
    private final SubscriberService subscriberService = mock(SubscriberService.class);
    private final CuratedListTrackingService curatedListTrackingService = mock(CuratedListTrackingService.class);
    private final EmailService emailService = mock(EmailService.class);
    private final StripeProcessedEventRepository processedEventRepository =
            mock(StripeProcessedEventRepository.class);
    private final StripeService service = new StripeService(
            stripeGateway,
            subscriberService,
            curatedListTrackingService,
            emailService,
            processedEventRepository,
            "sk_test_123",
            "price_test_123",
            "whsec_test_123",
            "https://dondeanime.com/premium?success=1",
            "https://dondeanime.com/premium?canceled=1",
            "https://dondeanime.com/premium");

    @Test
    void createCheckoutSessionBuildsStripeCommand() throws Exception {
        when(stripeGateway.createCheckoutSession(any(StripeCheckoutCommand.class)))
                .thenReturn("https://checkout.stripe.test/session");

        String url = service.createCheckoutSession(" Diego@Example.com ");

        assertThat(url).isEqualTo("https://checkout.stripe.test/session");
        ArgumentCaptor<StripeCheckoutCommand> captor = ArgumentCaptor.forClass(StripeCheckoutCommand.class);
        verify(stripeGateway).createCheckoutSession(captor.capture());
        assertThat(captor.getValue().email()).isEqualTo("diego@example.com");
        assertThat(captor.getValue().priceId()).isEqualTo("price_test_123");
    }

    @Test
    void createCheckoutSessionRequiresConfig() {
        StripeService unconfigured = new StripeService(
                stripeGateway,
                subscriberService,
                curatedListTrackingService,
                emailService,
                processedEventRepository,
                "",
                "price_test_123",
                "whsec_test_123",
                "success",
                "cancel",
                "portal");

        assertThatThrownBy(() -> unconfigured.createCheckoutSession("diego@example.com"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void createCheckoutSessionIncludesSourceListSlug() throws Exception {
        when(stripeGateway.createCheckoutSession(any(StripeCheckoutCommand.class)))
                .thenReturn("https://checkout.stripe.test/session");

        service.createCheckoutSession("diego@example.com", " Anime-Para-Empezar ");

        ArgumentCaptor<StripeCheckoutCommand> captor = ArgumentCaptor.forClass(StripeCheckoutCommand.class);
        verify(stripeGateway).createCheckoutSession(captor.capture());
        assertThat(captor.getValue().sourceListSlug()).isEqualTo("anime-para-empezar");
    }

    @Test
    void requestCustomerPortalLinkEmailsPortalUrlToActiveSubscriber() throws Exception {
        when(subscriberService.findActiveStripeCustomerId("diego@example.com"))
                .thenReturn(java.util.Optional.of("cus_test_123"));
        when(stripeGateway.createCustomerPortalSession(any(StripePortalCommand.class)))
                .thenReturn("https://billing.stripe.test/session");

        service.requestCustomerPortalLink(" Diego@Example.com ");

        ArgumentCaptor<StripePortalCommand> captor = ArgumentCaptor.forClass(StripePortalCommand.class);
        verify(stripeGateway).createCustomerPortalSession(captor.capture());
        assertThat(captor.getValue().customerId()).isEqualTo("cus_test_123");
        assertThat(captor.getValue().returnUrl()).isEqualTo("https://dondeanime.com/premium");
        verify(emailService).sendPremiumPortalEmail(
                "diego@example.com", "PREMIUM", "https://billing.stripe.test/session");
    }

    @Test
    void requestCustomerPortalLinkDoesNothingForUnknownEmailWithoutLeaking() throws Exception {
        when(subscriberService.findActiveStripeCustomerId("diego@example.com"))
                .thenReturn(java.util.Optional.empty());

        service.requestCustomerPortalLink("diego@example.com");

        verify(stripeGateway, never()).createCustomerPortalSession(any());
        verify(emailService, never()).sendPremiumPortalEmail(any(), any(), any());
    }

    @Test
    void handleWebhookActivatesSubscription() throws Exception {
        when(stripeGateway.constructWebhookEvent("{}", "sig", "whsec_test_123"))
                .thenReturn(new StripeWebhookEvent(
                        "customer.subscription.created",
                        "diego@example.com",
                        "cus_test_123",
                        "anime-para-empezar",
                        NOW,
                        NOW.plusSeconds(2_592_000),
                        "evt_created_1"));

        String received = service.handleWebhook("{}", "sig");

        assertThat(received).isEqualTo("customer.subscription.created");
        verify(subscriberService).upsertPremium(
                "diego@example.com",
                "cus_test_123",
                "PREMIUM",
                NOW,
                NOW.plusSeconds(2_592_000),
                null);
        verify(emailService).sendPremiumWelcomeEmail(
                "diego@example.com",
                "PREMIUM",
                "https://dondeanime.com/premium");
        verify(curatedListTrackingService).trackConversion("anime-para-empezar");
        verify(processedEventRepository).save(any(StripeProcessedEvent.class));
    }

    @Test
    void handleWebhookSkipsAlreadyProcessedEvent() throws Exception {
        when(stripeGateway.constructWebhookEvent("{}", "sig", "whsec_test_123"))
                .thenReturn(new StripeWebhookEvent(
                        "invoice.payment_succeeded",
                        "diego@example.com",
                        "cus_test_123",
                        null,
                        NOW,
                        null,
                        "evt_dup_1"));
        when(processedEventRepository.existsById("evt_dup_1")).thenReturn(true);

        String received = service.handleWebhook("{}", "sig");

        assertThat(received).isEqualTo("invoice.payment_succeeded");
        verify(subscriberService, never()).recordPaymentSucceeded(any(), any(), any());
        verify(emailService, never()).sendPremiumReceiptEmail(any(), any(), any(), any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void handleWebhookSendsReceiptForPaidInvoice() throws Exception {
        when(stripeGateway.constructWebhookEvent("{}", "sig", "whsec_test_123"))
                .thenReturn(new StripeWebhookEvent(
                        "invoice.payment_succeeded",
                        "diego@example.com",
                        "cus_test_123",
                        null,
                        NOW,
                        null,
                        "evt_invoice_1"));

        String received = service.handleWebhook("{}", "sig");

        assertThat(received).isEqualTo("invoice.payment_succeeded");
        verify(subscriberService).recordPaymentSucceeded("diego@example.com", "cus_test_123", NOW);
        verify(emailService).sendPremiumReceiptEmail(
                "diego@example.com",
                "PREMIUM",
                NOW.toString(),
                "https://dondeanime.com/premium");
    }

    @Test
    void handleWebhookCancelsSubscription() throws Exception {
        when(stripeGateway.constructWebhookEvent("{}", "sig", "whsec_test_123"))
                .thenReturn(new StripeWebhookEvent(
                        "customer.subscription.deleted",
                        null,
                        "cus_test_123",
                        null,
                        NOW,
                        null,
                        "evt_deleted_1"));

        String received = service.handleWebhook("{}", "sig");

        assertThat(received).isEqualTo("customer.subscription.deleted");
        verify(subscriberService).cancelByStripeCustomerId("cus_test_123", NOW);
    }

    @Test
    void verifyWebhookSignatureRejectsInvalidSignature() throws Exception {
        when(stripeGateway.constructWebhookEvent("{}", "bad", "whsec_test_123"))
                .thenThrow(new SignatureVerificationException("bad signature", "bad"));

        assertThatThrownBy(() -> service.verifyWebhookSignature("{}", "bad"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
