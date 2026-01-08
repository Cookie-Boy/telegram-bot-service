package ru.sibsutis.bot.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sibsutis.bot.core.model.TelegramUser;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TelegramUserRepository extends JpaRepository<TelegramUser, UUID> {
    TelegramUser findByUsername(String username);
    Optional<TelegramUser> findByChatId(String chatId);
}