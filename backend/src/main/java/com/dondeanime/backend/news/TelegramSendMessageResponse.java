package com.dondeanime.backend.news;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Respuesta de sendMessage de la Bot API de Telegram. Solo lo que usamos. */
@JsonIgnoreProperties(ignoreUnknown = true)
record TelegramSendMessageResponse(Boolean ok, Result result) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Result(@JsonProperty("message_id") Long messageId) {
    }
}
