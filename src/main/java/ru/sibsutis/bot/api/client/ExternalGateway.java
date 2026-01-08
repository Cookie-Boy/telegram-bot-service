package ru.sibsutis.bot.api.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.sibsutis.bot.api.dto.AppointmentResponseDto;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ExternalGateway {

    private final AppointmentServiceClient appointmentClient;

    public List<AppointmentResponseDto> getTgUserAppointments(UUID tgUserId) {
        return appointmentClient.getTgUserAppointments(tgUserId);
    }
}
