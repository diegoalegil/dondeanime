package com.dondeanime.backend.news;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Bot de Telegram para la revisión editorial de noticias: envía cada ítem
 * redactado por el LLM al chat de Diego con botones Publicar/Descartar.
 * Bot independiente del de alertas de scheduler (telegram.*): token y chat
 * propios bajo news.telegram.*, así ninguno pisa los getUpdates/webhook
 * del otro.
 */
@Service
@ConditionalOnProperty(name = "news.telegram.enabled", havingValue = "true")
public class TelegramNewsBotService {

    private static final Logger log = LoggerFactory.getLogger(TelegramNewsBotService.class);

    /** callback_data de los botones; máx. 64 bytes según Telegram, sobra. */
    static final String CALLBACK_PUBLISH_PREFIX = "news:pub:";
    static final String CALLBACK_DISCARD_PREFIX = "news:dis:";

    private final RestClient restClient;
    private final String botPath;
    private final String chatId;
    // Solo para censurarlo en logs: las excepciones de RestClient incluyen la
    // URL completa, y el token de Telegram va en el path.
    private final String botToken;

    public TelegramNewsBotService(
            RestClient.Builder restClientBuilder,
            @Value("${news.telegram.api-base:https://api.telegram.org}") String apiBase,
            @Value("${news.telegram.bot-token:}") String botToken,
            @Value("${news.telegram.chat-id:}") String chatId) {
        if (!StringUtils.hasText(botToken) || !StringUtils.hasText(chatId)) {
            throw new IllegalStateException(
                    "El bot de noticias está activado pero faltan news.telegram.bot-token o news.telegram.chat-id");
        }
        this.restClient = restClientBuilder.baseUrl(apiBase).build();
        this.botPath = "/bot" + botToken;
        this.chatId = chatId;
        this.botToken = botToken;
    }

    /**
     * Envía la solicitud de revisión con botones inline. Devuelve el
     * message_id, o null si Telegram falló: el ítem queda con
     * telegram_message_id NULL y la siguiente pasada lo reenvía.
     */
    public Long sendReviewRequest(NewsItem item) {
        Map<String, Object> request = Map.of(
                "chat_id", chatId,
                "text", formatReviewMessage(item),
                "disable_web_page_preview", true,
                "reply_markup", Map.of("inline_keyboard", List.of(List.of(
                        Map.of("text", "Publicar", "callback_data", CALLBACK_PUBLISH_PREFIX + item.getId()),
                        Map.of("text", "Descartar", "callback_data", CALLBACK_DISCARD_PREFIX + item.getId())))));
        try {
            TelegramSendMessageResponse response = restClient.post()
                    .uri(botPath + "/sendMessage")
                    .body(request)
                    .retrieve()
                    .body(TelegramSendMessageResponse.class);
            Long messageId = response == null || response.result() == null
                    ? null
                    : response.result().messageId();
            if (messageId == null) {
                log.warn("Telegram no devolvió message_id para la revisión de '{}'", item.getSlug());
            }
            return messageId;
        } catch (RestClientException e) {
            log.error("No se pudo enviar la revisión de '{}' a Telegram: {}", item.getSlug(), redacted(e));
            return null;
        }
    }

    /** Edita el mensaje original para quitar los botones y dejar el resultado. */
    public void markResolved(long messageId, NewsItem item, String resultado) {
        Map<String, Object> request = Map.of(
                "chat_id", chatId,
                "message_id", messageId,
                "text", formatReviewMessage(item) + "\n\n" + resultado);
        try {
            restClient.post()
                    .uri(botPath + "/editMessageText")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            // No bloquea la decisión: el estado ya cambió en BD.
            log.warn("No se pudo editar el mensaje {} de Telegram: {}", messageId, redacted(e));
        }
    }

    /** Responde el callback para que el cliente de Telegram deje de mostrar el spinner. */
    public void answerCallback(String callbackQueryId, String text) {
        try {
            restClient.post()
                    .uri(botPath + "/answerCallbackQuery")
                    .body(Map.of("callback_query_id", callbackQueryId, "text", text))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("No se pudo responder el callback {} de Telegram: {}", callbackQueryId, redacted(e));
        }
    }

    /** Mensaje de error sin el token (los errores de I/O incluyen la URL completa). */
    private String redacted(RestClientException e) {
        String message = e.getMessage();
        return message == null ? e.getClass().getSimpleName() : message.replace(botToken, "***");
    }

    private static String formatReviewMessage(NewsItem item) {
        StringBuilder text = new StringBuilder()
                .append("Nueva noticia para revisar\n\n")
                .append(item.getTitle());
        if (StringUtils.hasText(item.getSummary())) {
            text.append("\n\n").append(item.getSummary());
        }
        text.append("\n\nFuente: ").append(item.getSourceName())
                .append("\n").append(item.getSourceUrl());
        return text.toString();
    }
}
