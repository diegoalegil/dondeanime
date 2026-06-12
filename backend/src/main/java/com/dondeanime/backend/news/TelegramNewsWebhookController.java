package com.dondeanime.backend.news;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook de la Bot API: recibe los callbacks de los botones de revisión.
 * Webhook y no long-polling porque Caddy ya da TLS en api.dondeanime.com
 * (requisito único de setWebhook) y evita un hilo de getUpdates con offset
 * persistente. Seguridad: Telegram manda el secret_token registrado en
 * setWebhook en cada entrega; se compara en tiempo constante y sin él se
 * responde 401 sin cuerpo. El registro usa allowed_updates=["callback_query"]
 * para que solo lleguen pulsaciones de botones.
 */
@RestController
@ConditionalOnProperty(name = "news.telegram.enabled", havingValue = "true")
public class TelegramNewsWebhookController {

    private static final Logger log = LoggerFactory.getLogger(TelegramNewsWebhookController.class);
    static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final NewsReviewService reviewService;
    private final TelegramNewsBotService botService;
    private final byte[] webhookSecret;

    public TelegramNewsWebhookController(
            NewsReviewService reviewService,
            TelegramNewsBotService botService,
            @Value("${news.telegram.webhook-secret:}") String webhookSecret) {
        if (!StringUtils.hasText(webhookSecret)) {
            throw new IllegalStateException(
                    "El bot de noticias está activado pero falta news.telegram.webhook-secret");
        }
        this.reviewService = reviewService;
        this.botService = botService;
        this.webhookSecret = webhookSecret.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping("/api/news/telegram/webhook")
    public ResponseEntity<Void> onUpdate(
            @RequestHeader(value = SECRET_HEADER, required = false) String secretToken,
            @RequestBody TelegramUpdateDto update) {
        if (secretToken == null
                || !MessageDigest.isEqual(webhookSecret, secretToken.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // A partir de aquí siempre 200: ante un error de negocio Telegram
        // reintentaría la entrega en bucle y no hay nada que reintentar.
        try {
            handle(update);
        } catch (RuntimeException e) {
            log.error("Error procesando update de Telegram: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok().build();
    }

    private void handle(TelegramUpdateDto update) {
        if (update == null || update.callbackQuery() == null) {
            return;
        }
        TelegramUpdateDto.CallbackQuery callback = update.callbackQuery();
        String data = callback.data();
        if (data == null) {
            return;
        }

        boolean publish = data.startsWith(TelegramNewsBotService.CALLBACK_PUBLISH_PREFIX);
        boolean discard = data.startsWith(TelegramNewsBotService.CALLBACK_DISCARD_PREFIX);
        if (!publish && !discard) {
            return;
        }
        Long id = parseId(data);
        if (id == null) {
            log.warn("callback_data de revisión con id ilegible: '{}'", data);
            return;
        }

        NewsReviewService.ReviewDecision decision = publish
                ? reviewService.publish(id)
                : reviewService.discard(id);
        String answer = switch (decision.outcome()) {
            case PUBLISHED -> "Publicada";
            case DISCARDED -> "Descartada";
            case ALREADY_RESOLVED -> "Ya estaba gestionada";
            case NOT_FOUND -> "Noticia no encontrada";
        };

        if (callback.id() != null) {
            botService.answerCallback(callback.id(), answer);
        }
        boolean resolvedNow = decision.outcome() == NewsReviewService.ReviewOutcome.PUBLISHED
                || decision.outcome() == NewsReviewService.ReviewOutcome.DISCARDED;
        if (resolvedNow && decision.item() != null && decision.item().getTelegramMessageId() != null) {
            botService.markResolved(decision.item().getTelegramMessageId(), decision.item(), answer);
        }
    }

    private static Long parseId(String data) {
        int separator = data.lastIndexOf(':');
        try {
            return Long.parseLong(data.substring(separator + 1));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
