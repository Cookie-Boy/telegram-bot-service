package ru.sibsutis.bot.api.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestClient;

@Slf4j
public abstract class BaseServiceClient {

    protected final OAuth2AuthorizedClientManager clientManager;
    protected final RestClient restClient;
    protected final String serviceName;

    protected final String clientRegistrationId = "telegram-bot";

    protected BaseServiceClient(OAuth2AuthorizedClientManager clientManager,
                                RestClient restClient,
                                String serviceName) {
        this.clientManager = clientManager;
        this.restClient = restClient;
        this.serviceName = serviceName;
    }

    protected String getFreshToken() {
        OAuth2AuthorizedClient client = clientManager.authorize(
                OAuth2AuthorizeRequest.withClientRegistrationId(clientRegistrationId)
                        .principal("service-account")
                        .build()
        );

        return client != null ? client.getAccessToken().getTokenValue() : "token-is-null";
    }
}