package ru.sibsutis.bot.api.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.sibsutis.bot.api.dto.AppointmentResponseDto;

import java.util.List;

@Slf4j
@Component
public class AppointmentServiceClient extends BaseServiceClient {

    @Value("${endpoints.appointment.getUrl}")
    private String getUrl;

    @Autowired
    public AppointmentServiceClient(OAuth2AuthorizedClientManager clientManager,
                                    RestClient restClient,
                                    @Value("${endpoints.appointment.name}") String serviceName) {
        super(clientManager, restClient, serviceName);
    }

    public List<AppointmentResponseDto> getTgUserAppointments(String tgUserName) {
        try {
            String token = getFreshToken();
            log.info("Fresh token: {}", token);
            return restClient.get()
                    .uri(getUrl, tgUserName)
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RuntimeException e) {
            log.error("Failed to send a request to appointment-service: {}", String.valueOf(e));
            return null;
        }
    }
}
