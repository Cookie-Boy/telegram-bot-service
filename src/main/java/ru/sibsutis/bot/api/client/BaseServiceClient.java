package ru.sibsutis.bot.api.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

@Slf4j
public abstract class BaseServiceClient {

    protected final RestClient restClient;
    protected final String serviceName;

    protected BaseServiceClient(RestClient.Builder builder,
                                String baseUrl,
                                String serviceName) {
        this.serviceName = serviceName;
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("X-Source-Service", serviceName)
                .build();
    }
}