package com.dondeanime.backend.news;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

class TelegramNewsBotServiceConditionalTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(RestClient.Builder.class, RestClient::builder)
            .withUserConfiguration(TelegramNewsBotService.class);

    @Test
    void serviceIsNotCreatedWhenNewsTelegramIsDisabledByDefault() {
        contextRunner.run(context ->
                assertThat(context.getBeansOfType(TelegramNewsBotService.class)).isEmpty());
    }

    @Test
    void serviceIsCreatedWhenNewsTelegramIsEnabledAndConfigured() {
        contextRunner
                .withPropertyValues(
                        "news.telegram.enabled=true",
                        "news.telegram.bot-token=123456:news-token",
                        "news.telegram.chat-id=98765")
                .run(context ->
                        assertThat(context.getBeansOfType(TelegramNewsBotService.class)).hasSize(1));
    }

    @Test
    void enabledWithoutTokenFailsFast() {
        contextRunner
                .withPropertyValues("news.telegram.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }
}
