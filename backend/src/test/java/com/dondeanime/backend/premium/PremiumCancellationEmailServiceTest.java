package com.dondeanime.backend.premium;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dondeanime.backend.email.EmailService;

class PremiumCancellationEmailServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-25T10:00:00Z");

    private final SubscriberService subscriberService = mock(SubscriberService.class);
    private final EmailService emailService = mock(EmailService.class);
    private final PremiumCancellationEmailService service = new PremiumCancellationEmailService(
            subscriberService,
            emailService,
            Clock.fixed(NOW, ZoneOffset.UTC),
            "https://dondeanime.com/premium");

    @Test
    void sendsDueCancellationEmailAndMarksSubscriber() {
        Subscriber subscriber = subscriber();
        when(subscriberService.findDueCancellationEmails(NOW.minusSeconds(2_592_000)))
                .thenReturn(List.of(subscriber));

        int sent = service.sendDueCancellationEmails();

        assertThat(sent).isEqualTo(1);
        verify(emailService).sendPremiumCancellationEmail(
                "diego@example.com",
                "PREMIUM",
                "https://dondeanime.com/premium");
        verify(subscriberService).markCancellationEmailSent(10L, NOW);
    }

    @Test
    void doesNotMarkSubscriberWhenEmailFails() {
        Subscriber subscriber = subscriber();
        when(subscriberService.findDueCancellationEmails(NOW.minusSeconds(2_592_000)))
                .thenReturn(List.of(subscriber));
        doThrow(new RuntimeException("resend down"))
                .when(emailService)
                .sendPremiumCancellationEmail("diego@example.com", "PREMIUM", "https://dondeanime.com/premium");

        int sent = service.sendDueCancellationEmails();

        assertThat(sent).isZero();
        verify(subscriberService, never()).markCancellationEmailSent(10L, NOW);
    }

    private static Subscriber subscriber() {
        Subscriber subscriber = new Subscriber();
        subscriber.setId(10L);
        subscriber.setEmail("diego@example.com");
        subscriber.setPlanTier("PREMIUM");
        subscriber.setSubscribedAt(NOW.minusSeconds(5_184_000));
        subscriber.setExpiresAt(NOW.minusSeconds(2_592_000));
        return subscriber;
    }
}
