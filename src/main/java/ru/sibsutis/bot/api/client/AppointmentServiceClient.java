package ru.sibsutis.bot.api.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.sibsutis.bot.api.dto.AppointmentResponseDto;

import java.util.List;
import java.util.UUID;

@Component
public class AppointmentServiceClient extends BaseServiceClient {

    @Value("${clients.appointment.getUrl}")
    private String getUrl;

    public AppointmentServiceClient(RestClient.Builder builder,
                                    @Value("${clients.appointment.baseUrl}") String url,
                                    @Value("${clients.appointment.name}") String serviceName) {
        super(builder, url, serviceName);
    }

    public List<AppointmentResponseDto> getTgUserAppointments(UUID tgUserId) {
        return restClient.get()
                .uri(getUrl, tgUserId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
