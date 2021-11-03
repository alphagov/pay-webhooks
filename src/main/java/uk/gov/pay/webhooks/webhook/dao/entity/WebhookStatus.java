package uk.gov.pay.webhooks.webhook.dao.entity;

import java.util.stream.Stream;

public enum WebhookStatus {
    ACTIVE, INACTIVE;

    public static WebhookStatus of(String name) {
        return Stream.of(WebhookStatus.values())
                .filter(n -> n.getName().equals(name))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    public String getName() {
        return this.toString();
    }
}
