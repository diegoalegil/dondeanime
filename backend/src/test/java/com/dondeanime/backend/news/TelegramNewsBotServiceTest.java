package com.dondeanime.backend.news;

import static org.assertj.core.api.Assertions.assertThat;
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

class TelegramNewsBotServiceTest {

    private static TelegramNewsBotService serviceFor(RestClient.Builder builder) {
        return new TelegramNewsBotService(
                builder,
                "https://api.telegram.test",
                "123456:news-token",
                "98765");
    }

    @Test
    void sendsReviewRequestWithInlineButtonsAndReturnsMessageId() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramNewsBotService service = serviceFor(builder);

        server.expect(once(), requestTo("https://api.telegram.test/bot123456:news-token/sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.chat_id").value("98765"))
                .andExpect(jsonPath("$.text", containsString("Frieren tendra pelicula")))
                .andExpect(jsonPath("$.text", containsString("Fuente: ANN")))
                .andExpect(jsonPath("$.reply_markup.inline_keyboard[0][0].text").value("Publicar"))
                .andExpect(jsonPath("$.reply_markup.inline_keyboard[0][0].callback_data").value("news:pub:42"))
                .andExpect(jsonPath("$.reply_markup.inline_keyboard[0][1].text").value("Descartar"))
                .andExpect(jsonPath("$.reply_markup.inline_keyboard[0][1].callback_data").value("news:dis:42"))
                .andRespond(withSuccess("""
                        {"ok": true, "result": {"message_id": 111}}
                        """, MediaType.APPLICATION_JSON));

        Long messageId = service.sendReviewRequest(item());

        assertThat(messageId).isEqualTo(111L);
        server.verify();
    }

    @Test
    void sendFailureReturnsNullWithoutThrowing() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramNewsBotService service = serviceFor(builder);

        server.expect(once(), requestTo("https://api.telegram.test/bot123456:news-token/sendMessage"))
                .andRespond(withServerError());

        assertThat(service.sendReviewRequest(item())).isNull();
        server.verify();
    }

    @Test
    void markResolvedEditsOriginalMessage() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramNewsBotService service = serviceFor(builder);

        server.expect(once(), requestTo("https://api.telegram.test/bot123456:news-token/editMessageText"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.chat_id").value("98765"))
                .andExpect(jsonPath("$.message_id").value(111))
                .andExpect(jsonPath("$.text", containsString("Publicada")))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        assertThatCode(() -> service.markResolved(111L, item(), "Publicada"))
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void answerCallbackPostsCallbackQueryId() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramNewsBotService service = serviceFor(builder);

        server.expect(once(), requestTo("https://api.telegram.test/bot123456:news-token/answerCallbackQuery"))
                .andExpect(jsonPath("$.callback_query_id").value("cb-1"))
                .andExpect(jsonPath("$.text").value("Publicada"))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        assertThatCode(() -> service.answerCallback("cb-1", "Publicada"))
                .doesNotThrowAnyException();
        server.verify();
    }

    private static NewsItem item() {
        NewsItem item = new NewsItem();
        item.setId(42L);
        item.setSlug("frieren-movie");
        item.setTitle("Frieren anuncia pelicula");
        item.setSummary("Frieren tendra pelicula.");
        item.setSourceUrl("https://news.example/frieren");
        item.setSourceName("ANN");
        item.setStatus(NewsStatus.PENDING_REVIEW);
        return item;
    }
}
