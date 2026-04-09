package uk.gov.pay.webhooks.deliveryqueue.dao;

import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;

import java.time.Instant;

public class WebhookDeliveryQueueEntityFixture {

    private WebhookMessageEntity webhookMessageEntity;
    private final Instant sendAt = Instant.parse("2023-02-01T12:00:00Z");

    public static WebhookDeliveryQueueEntityFixture aWebhookDeliveryQueueEntity() {
        return new WebhookDeliveryQueueEntityFixture();
    }

    public WebhookDeliveryQueueEntityFixture withWebhookMessageEntity(WebhookMessageEntity webhookMessageEntity) {
        this.webhookMessageEntity = webhookMessageEntity;
        return this;
    }

    public WebhookDeliveryQueueEntity build() {
        var webhookDeliveryQueueEntity = new WebhookDeliveryQueueEntity();
        webhookDeliveryQueueEntity.setWebhookMessageEntity(webhookMessageEntity);
        webhookDeliveryQueueEntity.setSendAt(sendAt);
        return webhookDeliveryQueueEntity;
    }
}
