package com.dondeanime.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TelegramAlertServiceTest {

    @Test
    void sendsTelegramMessageWhenSchedulerJobFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramAlertService service = new TelegramAlertService(
                builder,
                "https://api.telegram.test",
                "123456:test-token",
                "98765");

        server.expect(once(), requestTo("https://api.telegram.test/bot123456:test-token/sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.chat_id").value("98765"))
                .andExpect(jsonPath("$.disable_web_page_preview").value(true))
                .andExpect(jsonPath("$.text", containsString("Job: providers")))
                .andExpect(jsonPath("$.text", containsString("IllegalStateException")))
                .andExpect(jsonPath("$.text", containsString("TMDb caído")))
                .andRespond(withSuccess("""
                        {"ok":true}
                        """, MediaType.APPLICATION_JSON));

        service.onSchedulerJobFailed(new SchedulerJobFailedEvent(
                "providers",
                new IllegalStateException("TMDb caído")));

        server.verify();
    }

    @Test
    void doesNotThrowWhenTelegramApiFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramAlertService service = new TelegramAlertService(
                builder,
                "https://api.telegram.test",
                "123456:test-token",
                "98765");

        server.expect(once(), requestTo("https://api.telegram.test/bot123456:test-token/sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThatCode(() -> service.onSchedulerJobFailed(new SchedulerJobFailedEvent(
                "anilist",
                new RuntimeException("AniList timeout"))))
                .doesNotThrowAnyException();

        server.verify();
    }
}
