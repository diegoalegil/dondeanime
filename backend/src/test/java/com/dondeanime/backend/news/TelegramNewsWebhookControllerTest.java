package com.dondeanime.backend.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class TelegramNewsWebhookControllerTest {

    private static final String SECRET = "webhook-secret";

    private final NewsReviewService reviewService = mock(NewsReviewService.class);
    private final TelegramNewsBotService botService = mock(TelegramNewsBotService.class);
    private final TelegramNewsWebhookController controller =
            new TelegramNewsWebhookController(reviewService, botService, SECRET);

    @Test
    void enabledWithoutWebhookSecretFailsFast() {
        assertThatThrownBy(() -> new TelegramNewsWebhookController(reviewService, botService, " "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("webhook-secret");
    }

    @Test
    void wrongSecretIsRejectedWithoutTouchingAnything() {
        ResponseEntity<Void> response = controller.onUpdate("otro-secreto", publishUpdate(42L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(reviewService, botService);
    }

    @Test
    void missingSecretIsRejected() {
        ResponseEntity<Void> response = controller.onUpdate(null, publishUpdate(42L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(reviewService, botService);
    }

    @Test
    void publishCallbackResolvesAnswersAndEditsMessage() {
        NewsItem item = new NewsItem();
        item.setId(42L);
        item.setTelegramMessageId(111L);
        when(reviewService.publish(42L)).thenReturn(
                new NewsReviewService.ReviewDecision(NewsReviewService.ReviewOutcome.PUBLISHED, item));

        ResponseEntity<Void> response = controller.onUpdate(SECRET, publishUpdate(42L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(reviewService).publish(42L);
        verify(botService).answerCallback("cb-1", "Publicada");
        verify(botService).markResolved(111L, item, "Publicada");
    }

    @Test
    void discardCallbackResolvesAsDiscarded() {
        NewsItem item = new NewsItem();
        item.setId(42L);
        item.setTelegramMessageId(111L);
        when(reviewService.discard(42L)).thenReturn(
                new NewsReviewService.ReviewDecision(NewsReviewService.ReviewOutcome.DISCARDED, item));

        controller.onUpdate(SECRET, new TelegramUpdateDto(1L,
                new TelegramUpdateDto.CallbackQuery("cb-1", "news:dis:42", null)));

        verify(reviewService).discard(42L);
        verify(botService).answerCallback("cb-1", "Descartada");
    }

    @Test
    void alreadyResolvedAnswersWithoutEditingMessage() {
        NewsItem item = new NewsItem();
        item.setId(42L);
        item.setTelegramMessageId(111L);
        when(reviewService.publish(42L)).thenReturn(
                new NewsReviewService.ReviewDecision(NewsReviewService.ReviewOutcome.ALREADY_RESOLVED, item));

        ResponseEntity<Void> response = controller.onUpdate(SECRET, publishUpdate(42L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(botService).answerCallback("cb-1", "Ya estaba gestionada");
        verify(botService, never()).markResolved(anyLong(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void irrelevantUpdatesReturnOkWithoutProcessing() {
        ResponseEntity<Void> noCallback = controller.onUpdate(SECRET, new TelegramUpdateDto(1L, null));
        ResponseEntity<Void> otherData = controller.onUpdate(SECRET, new TelegramUpdateDto(1L,
                new TelegramUpdateDto.CallbackQuery("cb-1", "otra:cosa", null)));
        ResponseEntity<Void> badId = controller.onUpdate(SECRET, new TelegramUpdateDto(1L,
                new TelegramUpdateDto.CallbackQuery("cb-1", "news:pub:xyz", null)));

        assertThat(noCallback.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(otherData.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(badId.getStatusCode()).isEqualTo(HttpStatus.OK);
        verifyNoInteractions(reviewService, botService);
    }

    @Test
    void serviceErrorsStillReturnOkSoTelegramDoesNotRetryForever() {
        when(reviewService.publish(42L)).thenThrow(new IllegalStateException("BD caida"));

        ResponseEntity<Void> response = controller.onUpdate(SECRET, publishUpdate(42L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private static TelegramUpdateDto publishUpdate(long id) {
        return new TelegramUpdateDto(1L,
                new TelegramUpdateDto.CallbackQuery("cb-1", "news:pub:" + id, null));
    }
}
