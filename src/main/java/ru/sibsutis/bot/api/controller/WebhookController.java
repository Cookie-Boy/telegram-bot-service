package ru.sibsutis.bot.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.sibsutis.bot.core.service.WebhookService;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/setup-webhook")
    public ResponseEntity<Map<String, String>> setupWebhook() {
        Map<String, String> response = new HashMap<>();

        try {
            log.info("Manually updating webhook...");
            webhookService.updateWebhook();

            response.put("status", "success");
            response.put("message", "Webhook setup complete!");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error setting webhook", e);

            response.put("status", "error");
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @GetMapping("/webhook-info")
    public ResponseEntity<Map<String, Object>> getWebhookInfo() {
        Map<String, Object> response = new HashMap<>();

        try {
            response.put("configured_url", webhookService.getWebhookUrl() + webhookService.getBotPath());
            response.put("status", "active");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @DeleteMapping("/webhook")
    public ResponseEntity<Map<String, String>> deleteWebhook() {
        Map<String, String> response = new HashMap<>();

        try {
            webhookService.deleteWebhook();

            response.put("status", "success");
            response.put("message", "Webhook deleted successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error deleting webhook", e);

            response.put("status", "error");
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }
}