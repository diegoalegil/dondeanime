package com.dondeanime.backend.premium;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

class PremiumCancellationEmailSchedulerTest {

    private final PremiumCancellationEmailService service = mock(PremiumCancellationEmailService.class);
    private final PremiumCancellationEmailScheduler scheduler = new PremiumCancellationEmailScheduler(service);

    @Test
    void delegatesToCancellationEmailService() {
        scheduler.sendPremiumCancellationEmails();

        verify(service).sendDueCancellationEmails();
    }

    @Test
    void doesNotPropagateErrors() {
        doThrow(new RuntimeException("resend down")).when(service).sendDueCancellationEmails();

        assertThatCode(scheduler::sendPremiumCancellationEmails).doesNotThrowAnyException();
    }
}
