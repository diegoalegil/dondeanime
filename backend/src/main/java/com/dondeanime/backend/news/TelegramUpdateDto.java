package com.dondeanime.backend.news;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Update de la Bot API de Telegram. Solo se modelan los callbacks de los
 * botones de revisión; cualquier otro tipo de update llega con
 * callback_query null y se ignora.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record TelegramUpdateDto(
        @JsonProperty("update_id") Long updateId,
        @JsonProperty("callback_query") CallbackQuery callbackQuery) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CallbackQuery(String id, String data, Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(@JsonProperty("message_id") Long messageId) {
    }
}
