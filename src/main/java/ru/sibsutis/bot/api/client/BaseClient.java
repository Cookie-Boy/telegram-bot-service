package ru.sibsutis.bot.api.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestClient;

@Slf4j
public abstract class BaseClient {


    protected final OAuth2AuthorizedClientManager clientManager;
    protected final RestClient restClient;
    protected final String targetServiceName;

    protected final String clientRegistrationId = "telegram-bot";

    protected BaseClient(OAuth2AuthorizedClientManager clientManager,
                         RestClient restClient,
                         String targetServiceName) {
        this.clientManager = clientManager;
        this.restClient = restClient;
        this.targetServiceName = targetServiceName;
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