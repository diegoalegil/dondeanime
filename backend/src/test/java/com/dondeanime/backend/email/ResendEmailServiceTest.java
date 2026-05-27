package com.dondeanime.backend.email;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ResendEmailServiceTest {

    @Test
    void sendsEmailThroughResendRestApi() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ResendEmailService service = new ResendEmailService(
                builder,
                "https://api.resend.com",
                "re_test",
                "alertas@dondeanime.com",
                true);

        server.expect(once(), requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer re_test"))
                .andExpect(jsonPath("$.from").value("alertas@dondeanime.com"))
                .andExpect(jsonPath("$.to[0]").value("diego@example.com"))
                .andExpect(jsonPath("$.subject").value("Attack on Titan ya está disponible en España"))
                .andRespond(withSuccess("""
                        {"id":"email_123"}
                        """, MediaType.APPLICATION_JSON));

        service.sendAlertEmail(
                "diego@example.com",
                "Attack on Titan",
                "España",
                List.of("Crunchyroll"),
                "https://api/unsubscribe",
                "https://api/erase");

        server.verify();
    }

    @Test
    void sendsPremiumReceiptEmailThroughResendRestApi() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ResendEmailService service = new ResendEmailService(
                builder,
                "https://api.resend.com",
                "re_test",
                "alertas@dondeanime.com",
                true);

        server.expect(once(), requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer re_test"))
                .andExpect(jsonPath("$.to[0]").value("diego@example.com"))
                .andExpect(jsonPath("$.subject").value("Recibo Premium DondeAnime"))
                .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("2026-05-25T10:00:00Z")))
                .andRespond(withSuccess("""
                        {"id":"email_456"}
                        """, MediaType.APPLICATION_JSON));

        service.sendPremiumReceiptEmail(
                "diego@example.com",
                "PREMIUM",
                "2026-05-25T10:00:00Z",
                "https://dondeanime.com/premium");

        server.verify();
    }

    @Test
    void sendsNewsletterConfirmationEmailThroughResendRestApi() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ResendEmailService service = new ResendEmailService(
                builder,
                "https://api.resend.com",
                "re_test",
                "alertas@dondeanime.com",
                true);

        server.expect(once(), requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer re_test"))
                .andExpect(jsonPath("$.to[0]").value("diego@example.com"))
                .andExpect(jsonPath("$.subject").value("Confirma tu newsletter de DondeAnime"))
                .andRespond(withSuccess("""
                        {"id":"email_789"}
                        """, MediaType.APPLICATION_JSON));

        service.sendNewsletterConfirmationEmail(
                "diego@example.com",
                "https://api/newsletter/confirm?token=raw.jwt");

        server.verify();
    }
}
