package com.dondeanime.backend.push;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class PushSubscriptionCleanupSchedulerTest {

    private final PushSubscriptionCleanupService cleanupService = mock(PushSubscriptionCleanupService.class);
    private final PushSubscriptionCleanupScheduler scheduler = new PushSubscriptionCleanupScheduler(cleanupService);

    @Test
    void purgeInactivePushSubscriptionsDelegatesToCleanupService() {
        when(cleanupService.purgeInactiveSubscriptions()).thenReturn(2);

        scheduler.purgeInactivePushSubscriptions();

        verify(cleanupService).purgeInactiveSubscriptions();
    }

    @Test
    void purgeInactivePushSubscriptionsDoesNotPropagateErrors() {
        doThrow(new IllegalStateException("db down")).when(cleanupService).purgeInactiveSubscriptions();

        assertThatCode(scheduler::purgeInactivePushSubscriptions).doesNotThrowAnyException();
    }
}
