package ru.sibsutis.bot.api.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class ProfileServiceClient extends BaseClient {

    @Value("${service.profile.url}")
    private String url;

    @Autowired
    public ProfileServiceClient(OAuth2AuthorizedClientManager clientManager,
                                RestClient restClient,
                                @Value("${service.profile.name}") String targetServiceName) {
        super(clientManager, restClient, targetServiceName);
    }

    public String getOwnerTgChatId(String petId) {
        try {
            String token = getFreshToken();
            log.info("Fresh token: {}", token);
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path(url)
                        .queryParam("petId", petId)
                        .build())
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RuntimeException e) {
            log.error("Failed to send a request to appointment-service: {}", String.valueOf(e));
            return null;
        }
    }
}

