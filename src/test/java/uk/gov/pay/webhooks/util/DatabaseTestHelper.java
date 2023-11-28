package uk.gov.pay.webhooks.util;

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
        jdbi.withHandle(h -> h.execute("INSERT INTO webhooks VALUES ('%d', '2022-01-01', '%s', 'signing-key', '%s', '%s', '%s', 'description', 'ACTIVE', '%s')".formatted(webhook.webhookId(), webhook.webhookExternalId(), webhook.serviceExternalId(), webhook.live(), webhook.endpointUrl(), webhook.gatewayAccountId())));
    }

    public void addWebhookSubscription(WebhookSubscription webhookSubscription) {
        jdbi.withHandle(h -> h.execute("INSERT INTO webhook_subscriptions VALUES ('%d', (SELECT id FROM event_types WHERE name = '%s'))".formatted(webhookSubscription.subscriptionId(), webhookSubscription.event())));
    }

    public void addWebhookMessage(WebhookMessage webhookMessage) {
        jdbi.withHandle(h -> h.execute("""
                INSERT INTO webhook_messages VALUES
                ('%d', '%s', '%s', '%d', '%s', '%d', '%s', '%s', '%s', '%s')
                """.formatted(webhookMessage.webhookMessageId(), webhookMessage.externalId(), webhookMessage.createdDate(), webhookMessage.webhookId(), webhookMessage.eventDate(), webhookMessage.eventType(), webhookMessage.resource(), webhookMessage.resourceExternalId(), webhookMessage.resourceType(), webhookMessage.deliveryStatus())));
    }

    public void addWebhookMessage(int startIdIndex, int recordCount, List<String> externalIdList, WebhookMessage webhookMessage) {
        for (int i = startIdIndex; i <= recordCount; i++) {
            addWebhookMessage(webhookMessage.withWebhookMessageAndExternalId(i,externalIdList.get(i - 2)));
        }
    }

    public void addWebhookDeliveryQueueMessage(WebhookDeliveryQueueMessage webhookDeliveryQueueMessage) {
        jdbi.withHandle(h -> h.execute("""
                INSERT INTO webhook_delivery_queue VALUES
                    ('%d', '%s', '%s', '%s', '%d', '%d', '%s', '%d')
                """.formatted(webhookDeliveryQueueMessage.deliveryQueueMessageId(), webhookDeliveryQueueMessage.sentDate(), webhookDeliveryQueueMessage.createdDate(), webhookDeliveryQueueMessage.deliveryResult(), webhookDeliveryQueueMessage.statusCode(), webhookDeliveryQueueMessage.webhookMessageId(), webhookDeliveryQueueMessage.deliveryStatus(), webhookDeliveryQueueMessage.deliveryCode())));
    }

    public void addWebhookDeliveryQueueMessage(int startIdIndex, int recordCount, WebhookDeliveryQueueMessage webhookDeliveryQueueMessage) {
        for (int i = startIdIndex; i <= recordCount; i++) {
            addWebhookDeliveryQueueMessage(webhookDeliveryQueueMessage.withDeliveryQueueAndWebhookMessageId(i,i - 2));
        }
    }

    public void truncateAllWebhooksData() {
        jdbi.withHandle(h -> h.createScript("TRUNCATE TABLE webhooks CASCADE; ").execute());
    }


    public record Webhook(int webhookId,
                                 String webhookExternalId,
                                 String serviceExternalId,
                                 String endpointUrl,
                                 String live,
                                 String gatewayAccountId) {
    }

    public record WebhookSubscription(int subscriptionId, String event) {
    }

    public record WebhookMessage(
            int webhookMessageId,
            String externalId,
            String createdDate,
            int webhookId,
            String eventDate,
            int eventType,
            String resource,
            String resourceExternalId,
            String resourceType,
            DeliveryStatus deliveryStatus) {
        public WebhookMessage withWebhookMessageAndExternalId(int webhookMessageId, String externalId) {
            return new WebhookMessage(webhookMessageId,
                    externalId,
                    createdDate(),
                    webhookId(),
                    eventDate(),
                    eventType(),
                    resource(),
                    resourceExternalId(),
                    resourceType(),
                    deliveryStatus());
        }
    }

    public record WebhookDeliveryQueueMessage(int deliveryQueueMessageId,
                                                     int webhookMessageId,
                                                     String sentDate,
                                                     String createdDate,
                                                     String deliveryResult,
                                                     int statusCode,
                                                     DeliveryStatus deliveryStatus,
                                                     int deliveryCode) {
        public WebhookDeliveryQueueMessage withDeliveryQueueAndWebhookMessageId(int deliveryQueueMessageId, int webhookMessageId) {
            return new WebhookDeliveryQueueMessage(
                    deliveryQueueMessageId,
                    webhookMessageId,
                    sentDate(),
                    createdDate(),
                    deliveryResult(),
                    statusCode(),
                    deliveryStatus(),
                    deliveryCode()
            );
        }
    }
}
