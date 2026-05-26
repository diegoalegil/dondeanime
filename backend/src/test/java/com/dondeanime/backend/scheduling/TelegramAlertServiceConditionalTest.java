package com.dondeanime.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

class TelegramAlertServiceConditionalTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(RestClient.Builder.class, RestClient::builder)
            .withUserConfiguration(TelegramAlertService.class);

    @Test
    void serviceIsNotCreatedWhenTelegramIsDisabledByDefault() {
        contextRunner.run(context ->
                assertThat(context.getBeansOfType(TelegramAlertService.class)).isEmpty());
    }

    @Test
    void serviceIsCreatedWhenTelegramIsEnabledAndConfigured() {
        contextRunner
                .withPropertyValues(
                        "telegram.enabled=true",
                        "telegram.bot-token=123456:test-token",
                        "telegram.chat-id=98765")
                .run(context ->
                        assertThat(context.getBeansOfType(TelegramAlertService.class)).hasSize(1));
    }
}
