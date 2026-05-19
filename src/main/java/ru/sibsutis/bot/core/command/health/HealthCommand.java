package ru.sibsutis.bot.core.command.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.sibsutis.bot.api.client.ExternalGateway;
import ru.sibsutis.bot.api.dto.LatestPetResultDto;
import ru.sibsutis.bot.api.dto.PetDto;
import ru.sibsutis.bot.core.annotation.NonStackable;
import ru.sibsutis.bot.core.command.BotCommand;
import ru.sibsutis.bot.core.model.VkMessage;
import ru.sibsutis.bot.core.service.MessageSender;

import java.util.List;

@Slf4j
@Component
@NonStackable
@RequiredArgsConstructor
public class HealthCommand implements BotCommand {

    private final MessageSender sender;
    private final ExternalGateway externalGateway;

    @Override
    public String getCommandName() {
        return "/health";
    }

    @Override
    public void execute(VkMessage message) {
        List<PetDto> pets = externalGateway.getPetsByVkUserId(message.getUserId());

        if (pets.isEmpty()) {
            sender.send(message.getUserId(), "🐾 У вас пока нет зарегистрированных питомцев.");
            return;
        }

        StringBuilder sb = new StringBuilder("❤️ Показатели здоровья\n\n");

        for (int i = 0; i < pets.size(); i++) {
            PetDto pet = pets.get(i);
            LatestPetResultDto result = externalGateway.getLatestVitals(pet.getId());

            sb.append(formatPetVitals(pet, result));

            if (i < pets.size() - 1) {
                sb.append("\n");
            }
        }

        sender.send(message.getUserId(), sb.toString());
    }

    private String formatPetVitals(PetDto pet, LatestPetResultDto result) {
        StringBuilder sb = new StringBuilder();

        // Имя и порода питомца
        sb.append(String.format("🐶 %s", pet.getName()));
        if (pet.getBreed() != null && !pet.getBreed().isEmpty()) {
            sb.append(String.format(" (%s)", pet.getBreed()));
        }
        sb.append("\n");

        // Если данных нет
        if (result == null) {
            sb.append("└─ Нет данных с ошейника\n");
            return sb.toString();
        }

        // Статус ошейника
        String statusIcon = "online".equalsIgnoreCase(result.getCollarStatus()) ? "🟢" : "🔴";
        String statusText = "online".equalsIgnoreCase(result.getCollarStatus()) ? "Онлайн" : "Офлайн";
        sb.append(String.format("└─ Статус: %s %s\n", statusIcon, statusText));

        // Пульс
        if (result.getHeartRate() != null) {
            String mark = "NORMAL".equals(result.getAnomalyReason()) ||
                    !"ABNORMAL_HEART_RATE".equals(result.getAnomalyReason()) ? "✅" : "❌";
            sb.append(String.format("   %s Пульс: %d уд/мин\n", mark, result.getHeartRate()));
        }

        // Дыхание
        if (result.getRespiratoryRate() != null) {
            String mark = "NORMAL".equals(result.getAnomalyReason()) ||
                    !"ABNORMAL_RESPIRATION".equals(result.getAnomalyReason()) ? "✅" : "❌";
            sb.append(String.format("   %s Дыхание: %d дых/мин\n", mark, result.getRespiratoryRate()));
        }

        // Температура
        if (result.getTemperature() != null) {
            String mark = "NORMAL".equals(result.getAnomalyReason()) ||
                    !"ABNORMAL_TEMPERATURE".equals(result.getAnomalyReason()) ? "✅" : "❌";
            sb.append(String.format("   %s Температура: %.1f°C\n", mark, result.getTemperature()));
        }

        // Активность
        if (result.getActivityLevel() != null) {
            String activityEmoji = getActivityEmoji(result.getActivityLevel());
            sb.append(String.format("   %s Активность: %d%%\n", activityEmoji, result.getActivityLevel()));
        }

        // Расстояние от дома
        if (result.getDistanceFromHome() != null) {
            String mark = !"TOO_FAR_FROM_HOME".equals(result.getAnomalyReason()) ? "🏠" : "⚠️";
            sb.append(String.format("   %s Расстояние от дома: %.2f м\n", mark, result.getDistanceFromHome()));
        }

        // Общий статус аномалии
        if (Boolean.TRUE.equals(result.getIsAnomalous())) {
            sb.append("   ⚠️ Обнаружена аномалия!\n");
            String reason = result.getAnomalyReason();
            if (reason != null && !reason.equals("NORMAL") && !reason.equals("UNKNOWN")) {
                sb.append(String.format("   Причина: %s\n", getReasonDescription(reason)));
            }
        } else {
            sb.append("   ✅ Все показатели в норме\n");
        }

        return sb.toString();
    }

    private String getReasonDescription(String reason) {
        return switch (reason) {
            case "ABNORMAL_HEART_RATE" -> "нарушение сердечного ритма";
            case "ABNORMAL_RESPIRATION" -> "нарушение дыхания";
            case "ABNORMAL_TEMPERATURE" -> "отклонение температуры";
            case "TOO_FAR_FROM_HOME" -> "питомец далеко от дома";
            default -> reason;
        };
    }

    private String getActivityEmoji(int level) {
        if (level < 20) return "😴";
        if (level < 50) return "🐢";
        if (level < 80) return "🏃";
        return "🚀";
    }
}
