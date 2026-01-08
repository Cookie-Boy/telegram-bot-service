package ru.sibsutis.bot.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Service
public class WebhookService {

    private final TelegramClient telegramClient;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${bot.token}")
    private String botToken;

    @Getter
    @Value("${bot.webhook.url}")
    private String webhookUrl;

    @Getter
    @Value("${bot.path}")
    private String botPath;

    @Autowired
    public WebhookService(@Value("${bot.token}") String botToken,
                          TelegramClient telegramClient) {
        this.botToken = botToken;
        this.telegramClient = telegramClient;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.telegram.org")
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public void checkAndUpdateWebhook() {
        try {
            String currentWebhookUrl = getCurrentWebhookUrl();
            String expectedUrl = webhookUrl + botPath;

            log.info("Current webhook URL: {}", currentWebhookUrl);
            log.info("Expected webhook URL: {}", expectedUrl);

            if (currentWebhookUrl == null || !currentWebhookUrl.equals(expectedUrl)) {
                log.info("Webhook URL mismatch. Updating...");
                updateWebhook();
            } else {
                log.info("Webhook URL is correct. No update needed.");
            }

        } catch (Exception e) {
            log.error("Error checking/updating webhook", e);
            try {
                updateWebhook();
            } catch (Exception ex) {
                log.error("Failed to update webhook after error", ex);
            }
        }
    }

    private String getCurrentWebhookUrl() {
        try {
            ResponseEntity<String> response = restClient.get()
                    .uri("/bot{token}/getWebhookInfo", botToken)
                    .retrieve()
                    .toEntity(String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.path("ok").asBoolean()) {
                    JsonNode result = root.path("result");
                    String url = result.path("url").asText(null);

                    log.info("Webhook info: url={}, pending_updates={}, last_error={}",
                            url,
                            result.path("pending_update_count").asInt(),
                            result.path("last_error_message").asText("none"));

                    return url;
                }
            }
        } catch (Exception e) {
            log.error("Error getting webhook info from Telegram", e);
        }
        return null;
    }

    public void updateWebhook() {
        try {
            log.info("Deleting old webhook...");
            telegramClient.execute(new DeleteWebhook());
            Thread.sleep(1000);

            log.info("Setting new webhook to: {}", webhookUrl + botPath);
            SetWebhook setWebhook = new SetWebhook(webhookUrl + botPath);
            telegramClient.execute(setWebhook);

            Thread.sleep(2000);
            verifyWebhook();

        } catch (TelegramApiException e) {
            log.error("Telegram API error while updating webhook", e);
            throw new RuntimeException("Failed to update webhook", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while updating webhook", e);
        }
    }

    private void verifyWebhook() {
        String currentUrl = getCurrentWebhookUrl();
        String expectedUrl = webhookUrl + botPath;

        if (expectedUrl.equals(currentUrl)) {
            log.info("✅ Webhook verified successfully: {}", currentUrl);
        } else {
            log.warn("⚠️ Webhook verification failed. Expected: {}, Got: {}",
                    expectedUrl, currentUrl);
        }
    }

    public void deleteWebhook() {
        try {
            log.info("Deleting webhook...");
            telegramClient.execute(new DeleteWebhook());
            log.info("Webhook deleted successfully");
        } catch (TelegramApiException e) {
            log.error("Error deleting webhook", e);
            throw new RuntimeException("Failed to delete webhook", e);
        }
    }
}