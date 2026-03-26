package ru.sibsutis.bot.api.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.sibsutis.bot.api.dto.AppointmentResponseDto;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ExternalGateway {

    private final AppointmentServiceClient appointmentClient;
    private final ProfileServiceClient profileServiceClient;

    public List<AppointmentResponseDto> getTgUserAppointments(String tgUserName) {
        return appointmentClient.getTgUserAppointments(tgUserName);
    }

    public String getOwnerTgChatId(String petId) {
        return profileServiceClient.getOwnerTgChatId(petId);
    }
}
