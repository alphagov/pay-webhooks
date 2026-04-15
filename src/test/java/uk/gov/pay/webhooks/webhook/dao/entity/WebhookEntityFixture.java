package uk.gov.pay.webhooks.webhook.dao.entity;

import static uk.gov.pay.webhooks.webhook.dao.entity.WebhookStatus.ACTIVE;

public class WebhookEntityFixture {

    private String callbackUrl;
    private final WebhookStatus webhookStatus = ACTIVE;
    private final String signingKey = "fake-signing-key";

    public static WebhookEntityFixture aWebhookEntity() {
        return new WebhookEntityFixture();
    }

    public WebhookEntityFixture withCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
        return this;
    }

    public WebhookEntity buld() {
        var webhookEntity = new WebhookEntity();
        webhookEntity.setCallbackUrl(callbackUrl);
        webhookEntity.setStatus(webhookStatus);
        webhookEntity.setSigningKey(signingKey);
        return webhookEntity;
    }
}
