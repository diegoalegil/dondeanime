package com.dondeanime.backend.premium;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.dondeanime.backend.email.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;

class PremiumAccessServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-25T10:00:00Z");

    private final SubscriberService subscriberService = mock(SubscriberService.class);
    private final EmailService emailService = mock(EmailService.class);
    private final PremiumAccessTokenService tokenService = new PremiumAccessTokenService(
            "test-secret",
            Clock.fixed(NOW, ZoneOffset.UTC),
            new ObjectMapper(),
            Duration.ofHours(1));
    private final PremiumAccessService service = new PremiumAccessService(
            subscriberService,
            tokenService,
            emailService,
            "https://dondeanime.com/");

    @Test
    void requestAccessLinkEmailsSignedTokenToActiveSubscriber() {
        when(subscriberService.findActiveEntitlement("diego@example.com"))
                .thenReturn(Optional.of(new PremiumEntitlement(
                        "diego@example.com",
                        "PREMIUM",
                        NOW.plusSeconds(3_600))));

        service.requestAccessLink(" Diego@Example.com ");

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPremiumAccessEmail(
                eq("diego@example.com"),
                eq("PREMIUM"),
                urlCaptor.capture());
        assertThat(urlCaptor.getValue()).startsWith("https://dondeanime.com/premium?access_token=");

        String token = URLDecoder.decode(
                urlCaptor.getValue().substring("https://dondeanime.com/premium?access_token=".length()),
                StandardCharsets.UTF_8);
        assertThat(tokenService.resolveEmail(token)).contains("diego@example.com");
    }

    @Test
    void requestAccessLinkDoesNothingForUnknownSubscriber() {
        when(subscriberService.findActiveEntitlement("diego@example.com"))
                .thenReturn(Optional.empty());

        service.requestAccessLink("diego@example.com");

        verify(emailService, never()).sendPremiumAccessEmail(
                any(),
                any(),
                any());
    }

    @Test
    void statusReturnsActiveOnlyForSignedTokenAndActiveSubscriber() {
        PremiumEntitlement entitlement = new PremiumEntitlement(
                "diego@example.com",
                "PREMIUM",
                NOW.plusSeconds(3_600));
        when(subscriberService.findActiveEntitlement("diego@example.com"))
                .thenReturn(Optional.of(entitlement));
        String token = tokenService.createToken(entitlement);

        PremiumStatusResponse status = service.status("Bearer " + token);

        assertThat(status.premium()).isTrue();
        assertThat(status.planTier()).isEqualTo("PREMIUM");
    }

    @Test
    void statusRejectsInvalidOrExpiredBearer() {
        assertThat(service.status("Bearer no").premium()).isFalse();
        assertThat(service.status(null).premium()).isFalse();
    }
}
