package ru.sibsutis.bot.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import ru.sibsutis.bot.core.service.TelegramBotService;

@Slf4j
@RestController
public class TelegramBotController {

    private final RestClient restClient;
    private final TelegramBotService telegramBotService;

    @Autowired
    public TelegramBotController(RestClient.Builder builder,
                                 @Value("${jetty.baseUrl}") String jettyBaseUrl,
                                 TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
        this.restClient = builder.baseUrl(jettyBaseUrl).build();
    }

    // Пользователь что-то написал →
    // Telegram сервер →
    // Публичный URL Tuna →
    // Spring Boot (localhost:8090/webhook) →
    // Jetty сервер (localhost:9091/webhook) →
    // TelegramBotService.consumeUpdate()
    @PostMapping("/webhook")
    public ResponseEntity<String> forwardToJetty(@RequestBody String payload,
                                            HttpHeaders headers) {
        MultiValueMap<String, String> forwardedHeaders = new HttpHeaders();
        forwardedHeaders.putAll(headers);

        try {
            String responseBody = restClient.post()
                    .uri(telegramBotService.getBotPath())
                    .headers(httpHeaders -> {
                        httpHeaders.putAll(forwardedHeaders);
                    })
                    .body(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new RuntimeException("Jetty error: " + response.getStatusCode());
                    })
                    .body(String.class);

            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            log.error("Error forwarding to Jetty: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error forwarding to Jetty");
        }
    }

    @PostMapping("/notify/{chatId}")
    public ResponseEntity<?> notifyUser(@PathVariable String chatId, @RequestBody String text) {
        boolean result = telegramBotService.sendNotification(chatId, text);
        return result ? ResponseEntity.ok().build() : ResponseEntity.internalServerError().build();
    }
}
