package com.dondeanime.backend.scheduling;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@ConditionalOnProperty(name = "telegram.enabled", havingValue = "true")
public class TelegramAlertService {

    private static final Logger log = LoggerFactory.getLogger(TelegramAlertService.class);
    private static final int MAX_ERROR_MESSAGE_LENGTH = 700;

    private final RestClient restClient;
    private final String sendMessagePath;
    private final String chatId;

    public TelegramAlertService(
            RestClient.Builder restClientBuilder,
            @Value("${telegram.api-base:https://api.telegram.org}") String apiBase,
            @Value("${telegram.bot-token:}") String botToken,
            @Value("${telegram.chat-id:}") String chatId) {
        if (!StringUtils.hasText(botToken) || !StringUtils.hasText(chatId)) {
            throw new IllegalStateException(
                    "Telegram está activado pero faltan telegram.bot-token o telegram.chat-id");
        }
        this.restClient = restClientBuilder.baseUrl(apiBase).build();
        this.sendMessagePath = "/bot" + botToken + "/sendMessage";
        this.chatId = chatId;
    }

    @EventListener
    public void onSchedulerJobFailed(SchedulerJobFailedEvent event) {
        Map<String, Object> request = Map.of(
                "chat_id", chatId,
                "text", formatMessage(event),
                "disable_web_page_preview", true);

        try {
            restClient.post()
                    .uri(sendMessagePath)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Alerta Telegram enviada para fallo de scheduler '{}'", event.job());
        } catch (RestClientException e) {
            log.error("No se pudo enviar alerta Telegram para scheduler '{}': {}", event.job(), e.getMessage());
        }
    }

    private static String formatMessage(SchedulerJobFailedEvent event) {
        Throwable error = event.error();
        String message = error.getMessage();
        if (!StringUtils.hasText(message)) {
            message = "(sin mensaje)";
        }

        return """
                DondeAnime: fallo crítico en scheduler
                Job: %s
                Error: %s
                Mensaje: %s
                """.formatted(
                event.job(),
                error.getClass().getSimpleName(),
                truncate(message, MAX_ERROR_MESSAGE_LENGTH))
                .trim();
    }

    private static String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}
