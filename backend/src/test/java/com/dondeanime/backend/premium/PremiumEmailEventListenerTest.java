package com.dondeanime.backend.premium;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.dondeanime.backend.email.EmailService;

class PremiumEmailEventListenerTest {

    private final EmailService emailService = mock(EmailService.class);
    private final PremiumEmailEventListener listener = new PremiumEmailEventListener(emailService);

    @Test
    void welcomeEventSendsWelcomeEmail() {
        listener.onPremiumEmail(PremiumEmailEvent.welcome(
                "diego@example.com", "PREMIUM", "https://dondeanime.com/premium"));

        verify(emailService).sendPremiumWelcomeEmail(
                "diego@example.com", "PREMIUM", "https://dondeanime.com/premium");
    }

    @Test
    void receiptEventSendsReceiptEmail() {
        listener.onPremiumEmail(PremiumEmailEvent.receipt(
                "diego@example.com", "PREMIUM", "2026-05-25T10:00:00Z", "https://dondeanime.com/premium"));

        verify(emailService).sendPremiumReceiptEmail(
                "diego@example.com", "PREMIUM", "2026-05-25T10:00:00Z", "https://dondeanime.com/premium");
    }

    @Test
    void emailFailureAfterCommitIsLoggedNotPropagated() {
        // El alta Premium ya está commiteada: una caída de Resend no puede
        // romper la respuesta del webhook.
        doThrow(new IllegalStateException("Resend caido"))
                .when(emailService).sendPremiumWelcomeEmail("diego@example.com", "PREMIUM", "url");

        assertThatCode(() -> listener.onPremiumEmail(
                PremiumEmailEvent.welcome("diego@example.com", "PREMIUM", "url")))
                .doesNotThrowAnyException();
    }
}
