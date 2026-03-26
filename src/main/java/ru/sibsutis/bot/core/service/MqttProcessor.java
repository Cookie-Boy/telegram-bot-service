package ru.sibsutis.bot.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import ru.sibsutis.bot.api.client.ExternalGateway;
import ru.sibsutis.bot.core.model.AnalysisResult;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttProcessor {

    private final ExternalGateway externalGateway;
    private final ObjectMapper objectMapper;
    private final TelegramBotService telegramBotService;

    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public void handleVitalData(Message<String> message) {
        String payload = message.getPayload();
        try {
            AnalysisResult result = objectMapper.readValue(payload, AnalysisResult.class);

            String tgChatId = externalGateway.getOwnerTgChatId(result.getPetId());
            String messageText = generateMessageText(result);

            telegramBotService.sendMessage(tgChatId, messageText);

            log.info("Notification sent to owner for petId: {}, anomalyType: {}",
                    result.getPetId(), result.getAnomalyType());
        } catch (Exception e) {
            log.error("Error processing MQTT message: {}", e.getMessage());
        }
    }

    private String generateMessageText(AnalysisResult result) {
        StringBuilder message = new StringBuilder();

        message.append("🚨 **ВНИМАНИЕ!** 🚨\n\n");
        message.append("Обнаружены аномальные показатели у вашего питомца!\n\n");

        switch (result.getAnomalyType()) {
            case ABNORMAL_HEART_RATE:
                message.append("❤️ **Проблема с сердечным ритмом**\n");
                Object heartRate = result.getDetails().get("heartRate");
                if (heartRate != null) {
                    message.append(String.format("Частота пульса: %.1f уд/мин\n", ((Number) heartRate).doubleValue()));
                }
                message.append("⚠️ Пульс выходит за пределы нормы.\n");
                message.append("📌 Рекомендация: обратитесь к ветеринару для проверки сердца.\n");
                break;

            case ABNORMAL_RESPIRATION:
                message.append("🌬️ **Проблема с дыханием**\n");
                Object respiration = result.getDetails().get("respiration");
                if (respiration != null) {
                    message.append(String.format("Частота дыхания: %.1f вдохов/мин\n", ((Number) respiration).doubleValue()));
                }
                message.append("⚠️ Дыхание учащённое или замедленное.\n");
                message.append("📌 Рекомендация: проверьте дыхательные пути, обеспечьте покой питомцу.\n");
                break;

            case ABNORMAL_TEMPERATURE:
                message.append("🌡️ **Проблема с температурой тела**\n");
                Object temperature = result.getDetails().get("temperature");
                if (temperature != null) {
                    message.append(String.format("Температура тела: %.1f°C\n", ((Number) temperature).doubleValue()));
                }
                message.append("⚠️ Температура выходит за пределы нормы.\n");
                message.append("📌 Рекомендация: измерьте температуру повторно, при необходимости обратитесь к врачу.\n");
                break;

            case TOO_FAR_FROM_HOME:
                message.append("📍 **Питомец убежал слишком далеко!**\n");
                Object distance = result.getDetails().get("distance");
                if (distance != null) {
                    message.append(String.format("Расстояние от дома: %.0f м\n", ((Number) distance).doubleValue()));
                }
                message.append("⚠️ Питомец покинул безопасную зону!\n");
                message.append("📌 Рекомендация: немедленно проверьте местоположение питомца по GPS.\n");
                break;

            default:
                message.append("❓ **Неизвестная аномалия**\n");
                message.append("⚠️ Обнаружены отклонения в показателях здоровья.\n");
                message.append("📌 Рекомендация: обратитесь к ветеринару для обследования.\n");
                break;
        }

        message.append("\n🤖 *Это автоматическое сообщение от системы мониторинга здоровья питомца.*");
        message.append("\n⏰ Время обнаружения: ").append(formatTimestamp(result.getTimestamp()));

        return message.toString();
    }

    private String formatTimestamp(long timestamp) {
        Instant instant = Instant.ofEpochSecond(timestamp);
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                instant, java.time.ZoneId.systemDefault()
        );
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        return dateTime.format(formatter);
    }

}
