package uk.gov.pay.webhooks.util;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.jdbi.v3.core.Jdbi;
import uk.gov.pay.webhooks.deliveryqueue.DeliveryStatus;

import java.util.List;

/*
  Group methods referencing same database tables together for ease of maintenance and future refactor .
  e.g. webhooks database table inserts are arranged at the top of the file.
 */
public class DatabaseTestHelper {
    private final Jdbi jdbi;

    private DatabaseTestHelper(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public static DatabaseTestHelper aDatabaseTestHelper(Jdbi jdbi) {
        return new DatabaseTestHelper(jdbi);
    }

    public void addWebhook(Webhook webhook) {
        jdbi.withHandle(h -> h.execute("INSERT INTO webhooks VALUES ('%d', '2022-01-01', '%s', 'signing-key', '%s', '%s', '%s', 'description', 'ACTIVE', '%s')"
                .formatted(webhook.getWebhookId(),
                        webhook.getWebhookExternalId(),
                        webhook.getServiceExternalId(),
                        webhook.getLive(),
                        webhook.getEndpointUrl(),
                        webhook.getGatewayAccountId())));
    }

    public void addWebhookSubscription(WebhookSubscription webhookSubscription) {
        jdbi.withHandle(h -> h.execute("INSERT INTO webhook_subscriptions VALUES ('%d', (SELECT id FROM event_types WHERE name = '%s'))"
                .formatted(webhookSubscription.getSubscriptionId(),
                        webhookSubscription.getEvent())));
    }

    public void addWebhookMessage(WebhookMessage webhookMessage) {
        jdbi.withHandle(h -> h.execute("""
                INSERT INTO webhook_messages VALUES
                ('%d', '%s', '%s', '%d', '%s', '%d', '%s', '%s', '%s', '%s')
                """.formatted(
                webhookMessage.getWebhookMessageId(),
                webhookMessage.getExternalId(),
                webhookMessage.getCreatedDate(),
                webhookMessage.getWebhookId(),
                webhookMessage.getEventDate(),
                webhookMessage.getEventType(),
                webhookMessage.getResource(),
                webhookMessage.getResourceExternalId(),
                webhookMessage.getResourceType(),
                webhookMessage.getDeliveryStatus())
        ));
    }

    public void addWebhookMessage(int startIdIndex, int recordCount, List<String> externalIdList, WebhookMessage webhookMessage) {
        for (int i = startIdIndex; i <= recordCount; i++) {
            webhookMessage.setWebhookMessageId(i);
            webhookMessage.setExternalId(externalIdList.get(i - 2));
            addWebhookMessage(webhookMessage);
        }
    }

    public void addWebhookDeliveryQueueMessage(WebhookDeliveryQueueMessage webhookDeliveryQueueMessage) {
        jdbi.withHandle(h -> h.execute("""
                INSERT INTO webhook_delivery_queue VALUES
                    ('%d', '%s', '%s', '%s', '%d', '%d', '%s', '%d')
                """.formatted(webhookDeliveryQueueMessage.getDeliveryQueueMessageId(),
                webhookDeliveryQueueMessage.getSentDate(),
                webhookDeliveryQueueMessage.getCreatedDate(),
                webhookDeliveryQueueMessage.getDeliveryResult(),
                webhookDeliveryQueueMessage.getStatusCode(),
                webhookDeliveryQueueMessage.getWebhookMessageId(),
                webhookDeliveryQueueMessage.getDeliveryStatus(),
                webhookDeliveryQueueMessage.getDeliveryCode())
        ));
    }

    public void addWebhookDeliveryQueueMessage(int startIdIndex, int recordCount, WebhookDeliveryQueueMessage webhookDeliveryQueueMessage) {
        for (int i = startIdIndex; i <= recordCount; i++) {
            webhookDeliveryQueueMessage.setDeliveryQueueMessageId(i);
            webhookDeliveryQueueMessage.setWebhookMessageId(i - 2);
            addWebhookDeliveryQueueMessage(webhookDeliveryQueueMessage);
        }
    }

    public void truncateAllWebhooksData() {
        jdbi.withHandle(h -> h.createScript(
                "TRUNCATE TABLE webhooks CASCADE; "
        ).execute());
    }

    @Getter
    @Builder
    public static class Webhook {
        private final int webhookId;
        private final String webhookExternalId;
        private final String serviceExternalId;
        private final String endpointUrl;
        private final String live;
        private final String gatewayAccountId;
    }

    @Getter
    @Builder
    public static class WebhookSubscription {
        private final int subscriptionId;
        private final String event;
    }

    @Getter
    @Setter
    @Builder
    public static class WebhookMessage {
        private int webhookMessageId;
        private String externalId;
        private final String createdDate;
        private final int webhookId;
        private final String eventDate;
        private final int eventType;
        private final String resource;
        private final String resourceExternalId;
        private final String resourceType;
        private final DeliveryStatus deliveryStatus;
    }

    @Getter
    @Setter
    @Builder
    public static class WebhookDeliveryQueueMessage {
        private int deliveryQueueMessageId;
        private int webhookMessageId;
        private final String sentDate;
        private final String createdDate;
        private final String deliveryResult;
        private final int statusCode;
        private final DeliveryStatus deliveryStatus;
        private final int deliveryCode;
    }
}
