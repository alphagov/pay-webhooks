package uk.gov.pay.webhooks.webhook.dao.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.stream.Stream;

public enum WebhookStatus {

    @JsonProperty("active")
    ACTIVE("active"),
    @JsonProperty("inactive")
    INACTIVE("inactive");

    private final String name;

    WebhookStatus(String name) {
        this.name = name;
    }

    public static WebhookStatus of(String name) {
        return Stream.of(WebhookStatus.values())
                .filter(n -> n.getName().equals(name))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    public String getName() {
        return this.name;
    }
}
