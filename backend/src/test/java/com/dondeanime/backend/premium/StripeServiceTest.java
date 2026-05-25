package com.dondeanime.backend.premium;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.stripe.exception.SignatureVerificationException;

class StripeServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-25T10:00:00Z");

    private final StripeGateway stripeGateway = mock(StripeGateway.class);
    private final SubscriberService subscriberService = mock(SubscriberService.class);
    private final StripeService service = new StripeService(
            stripeGateway,
            subscriberService,
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
    void createCustomerPortalSessionBuildsStripeCommand() throws Exception {
        when(subscriberService.findActiveStripeCustomerId("diego@example.com"))
                .thenReturn(java.util.Optional.of("cus_test_123"));
        when(stripeGateway.createCustomerPortalSession(any(StripePortalCommand.class)))
                .thenReturn("https://billing.stripe.test/session");

        String url = service.createCustomerPortalSession(" Diego@Example.com ");

        assertThat(url).isEqualTo("https://billing.stripe.test/session");
        ArgumentCaptor<StripePortalCommand> captor = ArgumentCaptor.forClass(StripePortalCommand.class);
        verify(stripeGateway).createCustomerPortalSession(captor.capture());
        assertThat(captor.getValue().customerId()).isEqualTo("cus_test_123");
        assertThat(captor.getValue().returnUrl()).isEqualTo("https://dondeanime.com/premium");
    }

    @Test
    void createCustomerPortalSessionReturnsNotFoundForUnknownPremiumEmail() {
        when(subscriberService.findActiveStripeCustomerId("diego@example.com"))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.createCustomerPortalSession("diego@example.com"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void handleWebhookActivatesSubscription() throws Exception {
        when(stripeGateway.constructWebhookEvent("{}", "sig", "whsec_test_123"))
                .thenReturn(new StripeWebhookEvent(
                        "customer.subscription.created",
                        "diego@example.com",
                        "cus_test_123",
                        NOW,
                        NOW.plusSeconds(2_592_000)));

        String received = service.handleWebhook("{}", "sig");

        assertThat(received).isEqualTo("customer.subscription.created");
        verify(subscriberService).upsertPremium(
                "diego@example.com",
                "cus_test_123",
                "PREMIUM",
                NOW,
                NOW.plusSeconds(2_592_000),
                null);
    }

    @Test
    void handleWebhookCancelsSubscription() throws Exception {
        when(stripeGateway.constructWebhookEvent("{}", "sig", "whsec_test_123"))
                .thenReturn(new StripeWebhookEvent(
                        "customer.subscription.deleted",
                        null,
                        "cus_test_123",
                        NOW,
                        null));

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
