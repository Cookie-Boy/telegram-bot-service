package ru.sibsutis.bot.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.webhook.TelegramBotsWebhookApplication;
import org.telegram.telegrambots.webhook.WebhookOptions;
import ru.sibsutis.bot.core.service.TelegramBotService;

@Configuration
public class TelegramConfig {

    @Bean
    public TelegramBotsWebhookApplication telegramBotsApi(TelegramBotService botService) throws TelegramApiException {
        TelegramBotsWebhookApplication webhookApp =
                new TelegramBotsWebhookApplication(
                        WebhookOptions.builder()
                                .enableRequestLogging(true)
                                .build()
                );

        webhookApp.registerBot(botService);
        return webhookApp;
    }

    @Bean
    public TelegramClient telegramClient(@Value("${bot.token}") String botToken) {
        return new OkHttpTelegramClient(botToken);
    }
}
