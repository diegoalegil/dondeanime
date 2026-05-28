package com.dondeanime.backend.premium;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PremiumController.class)
class PremiumControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private StripeService stripeService;

    @Test
    void checkoutReturnsStripeUrl() throws Exception {
        when(stripeService.createCheckoutSession("diego@example.com"))
                .thenReturn("https://checkout.stripe.test/session");

        mvc.perform(post("/api/premium/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"diego@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://checkout.stripe.test/session"));

        verify(stripeService).createCheckoutSession("diego@example.com");
    }

    @Test
    void checkoutRejectsInvalidEmail() throws Exception {
        mvc.perform(post("/api/premium/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-es-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void portalReturnsGenericStatusAndRequestsPortalLink() throws Exception {
        mvc.perform(post("/api/premium/portal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"diego@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("email_sent"));

        verify(stripeService).requestCustomerPortalLink("diego@example.com");
    }

    @Test
    void portalRejectsInvalidEmail() throws Exception {
        mvc.perform(post("/api/premium/portal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-es-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void webhookReturnsReceivedType() throws Exception {
        when(stripeService.handleWebhook("{\"id\":\"evt_test\"}", "sig_test"))
                .thenReturn("invoice.payment_succeeded");

        mvc.perform(post("/api/premium/webhook")
                        .header("Stripe-Signature", "sig_test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"evt_test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value("invoice.payment_succeeded"));

        verify(stripeService).handleWebhook("{\"id\":\"evt_test\"}", "sig_test");
    }
}
