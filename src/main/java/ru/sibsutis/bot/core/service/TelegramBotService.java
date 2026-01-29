package ru.sibsutis.bot.core.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.webhook.TelegramWebhookBot;
import ru.sibsutis.bot.api.client.ExternalGateway;
import ru.sibsutis.bot.api.dto.AppointmentResponseDto;
import ru.sibsutis.bot.core.model.TelegramUser;
import ru.sibsutis.bot.core.repository.TelegramUserRepository;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TelegramBotService implements TelegramWebhookBot {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final TelegramClient telegramClient;
    private final WebhookService webhookService;
    private final ExternalGateway externalGateway;
    private final TelegramUserRepository telegramUserRepository;

    private final Map<String, Function<Message, BotApiMethod<?>>> commandMap = new HashMap<>();

    @Value("${bot.name}")
    private String name;

    @Value("${bot.path}")
    private String path;

    @Value("${bot.webhook.url}")
    private String url;

    @Autowired
    public TelegramBotService(@Value("${bot.token}") String token,
                              TelegramUserRepository telegramUserRepository,
                              ExternalGateway externalGateway,
                              WebhookService webhookService,
                              TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
        this.telegramUserRepository = telegramUserRepository;
        this.externalGateway = externalGateway;
        this.webhookService = webhookService;
    }

    @PostConstruct
    public void init() {
        commandMap.put("/start", this::handleStartCommand);
        commandMap.put("/schedule", this::handleScheduleCommand);
        webhookService.getCurrentWebhookUrl();
    }

    private void scheduleWebhookCheck() {
        new Thread(() -> {
            try {
                Thread.sleep(10000);
                log.info("Starting automatic webhook check...");
                webhookService.checkAndUpdateWebhook();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Webhook check interrupted", e);
            } catch (Exception e) {
                log.error("Error in automatic webhook check", e);
            }
        }).start();
    }

    @Override
    public void runDeleteWebhook() {
        try {
            telegramClient.execute(new DeleteWebhook());
        } catch (TelegramApiException e) {
            log.info("Error deleting webhook");
        }
    }

    @Override
    public void runSetWebhook() {
        try {
            telegramClient.execute(new SetWebhook(url + path));
        } catch (TelegramApiException e) {
            log.info("Error setting webhook: {}", String.valueOf(e));
        }
    }

    public boolean sendNotification(String username, String message) {
        TelegramUser user = telegramUserRepository.findByUsername(username);
        String chatId = user.getChatId();
        SendMessage sendMessage = new SendMessage(chatId, message);
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);

        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки уведомления", e);
            return false;
        }

        return true;
    }

    @Override
    public String getBotPath() {
        return path;
    }

    @Override
    public BotApiMethod<?> consumeUpdate(Update update) {
        log.info("Got the update");
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return null;
        }

        Message message = update.getMessage();
        if (!message.getText().startsWith("/")) {
            return sendMessage(message.getChatId().toString(), "You should write a command.");
        }

        Function<Message, BotApiMethod<?>> handler = commandMap.get(message.getText());

        if (handler != null) {
            return handler.apply(message);
        }

        return sendMessage(message.getChatId().toString(), "Unknown command...");
    }

    public SendMessage sendMessage(String chatId, String text) {
        SendMessage sendMessage = new SendMessage(chatId, text);
        try {
            telegramClient.execute(sendMessage);
            return sendMessage;
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private BotApiMethod<?> handleStartCommand(Message message) {
        String chatId = message.getChatId().toString();
        String username = message.getFrom().getUserName();

        TelegramUser user = telegramUserRepository.findByChatId(chatId).orElseGet(() -> telegramUserRepository.save(TelegramUser.builder()
                .username(username)
                .chatId(chatId)
                .build()));

        log.info("Message from user with chatId: {}", user.getChatId());

        return sendMessage(chatId, "Привет! Я бот, который поможет тебе забронировать прием у врача.\n"
                + "Доступные команды:\n"
                + "/schedule - показать все записи\n"
                + "/book - создать новую запись");
    }

    public BotApiMethod<?> handleScheduleCommand(Message message) {
        String chatId = message.getChatId().toString();
        TelegramUser user = telegramUserRepository.findByChatId(chatId)
                .orElse(null);
        if (user == null) {
            return sendMessage(chatId, "Сначала зарегистрируйтесь с помощью /start");
        }

        List<AppointmentResponseDto> appointments = externalGateway.getTgUserAppointments("@" + user.getUsername());

        if (appointments == null) {
            return sendMessage(chatId, "Из-за технических неполадок сервис 'appointments' недоступен, ожидайте...");
        }

        if (appointments.isEmpty()) {
            return sendMessage(chatId, "У вас нет запланированных приёмов.");
        }

        String schedule = appointments.stream()
                .map(this::formatAppointment)
                .collect(Collectors.joining("\n\n"));

        return sendMessage(chatId, "Ваши записи:\n\n" + schedule);
    }

    private String formatAppointment(AppointmentResponseDto appointment) {
        return String.format(
                """
                📅 %s в %s
                👨⚕️ Врач: %s
                🏥 Клиника: %s
                🔖 Статус: %s""",
                appointment.startTime().format(DATE_FORMATTER),
                appointment.startTime().format(TIME_FORMATTER),
                appointment.doctorFullName(),
                appointment.clinicName(),
                getStatusEmoji(appointment.status())
        );
    }

    private String getStatusEmoji(String status) {
        return switch (status) {
            case "CONFIRMED" -> "✅ Подтверждено";
            case "PENDING" -> "⏳ Ожидает подтверждения";
            case "CANCELLED" -> "❌ Отменено";
            default -> "";
        };
    }
}
