package uk.gov.pay.webhooks.message.dao.entity;

import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import java.time.Instant;

public class WebhookMessageEntityFixture {

    private WebhookEntity webhookEntity;
    private final Instant eventDate = Instant.parse("2022-01-01T00:00:00Z");
    private final EventTypeEntity eventType = new EventTypeEntity();

    public static WebhookMessageEntityFixture aWebhookMessageEntity() {
        return new WebhookMessageEntityFixture();
    }

    public WebhookMessageEntityFixture withWebhookEntity(WebhookEntity entity) {
        this.webhookEntity = entity;
        return this;
    }

    public WebhookMessageEntity build() {
        var webhookMessageEntity = new WebhookMessageEntity();
        webhookMessageEntity.setWebhookEntity(webhookEntity);
        webhookMessageEntity.setEventDate(eventDate);
        webhookMessageEntity.setEventType(eventType);
        return webhookMessageEntity;
    }
}
