package uk.gov.pay.webhooks.util;

import org.jdbi.v3.core.Jdbi;
import uk.gov.pay.webhooks.deliveryqueue.DeliveryStatus;

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

    public void addWebhook() {
        jdbi.withHandle(h -> h.execute("INSERT INTO webhooks VALUES (1, '2022-01-01', 'webhook-external-id', 'signing-key', 'service-id', true, 'https://callback-url.test', 'description', 'ACTIVE')"));
    }
    public void addWebhook(String externalId) {
        jdbi.withHandle(h -> h.execute("INSERT INTO webhooks VALUES (1, '2022-01-01', '%s', 'signing-key', 'service-id', true, 'http://callback-url.com', 'description', 'ACTIVE', '100')".formatted(externalId)));
    }
    public void addWebhook(String webhookExternalId, String serviceExternalId, String callbackUrl, String gatewayAccountId) {
        jdbi.withHandle(h -> h.execute("INSERT INTO webhooks VALUES (1, '2022-01-01', '%s', 'signing-key', '%s', false, '%s', 'description', 'ACTIVE', '%s')".formatted(webhookExternalId, serviceExternalId, callbackUrl, gatewayAccountId)));
    }
    public void addWebhook(int webhookId, String webhookExternalId, String serviceExternalId, String endpointUrl, String gatewayAccountId) {
        jdbi.withHandle(h -> h.execute("INSERT INTO webhooks VALUES ('%d', '2022-01-01', '%s', 'signing-key', '%s', false, '%s', 'description', 'ACTIVE', '%s')"
                .formatted(webhookId, webhookExternalId, serviceExternalId, endpointUrl, gatewayAccountId)));
    }
    public void addWebhookSubscription(int webhookSubscriptionId, String event) {
        jdbi.withHandle(h -> h.execute("INSERT INTO webhook_subscriptions VALUES ('%d', (SELECT id FROM event_types WHERE name = '%s'))".formatted(webhookSubscriptionId, event)));
    }
    
    
    public void addWebhookMessage(int webhookMessageId, String externalId, String createdDate, int webhookId, String eventDate, int eventType, String resource, String resourceExternalId, String resourceType, DeliveryStatus status) {
        jdbi.withHandle(h -> h.execute("""
                INSERT INTO webhook_messages VALUES
                ('%d', '%s', '%s', '%d', '%s', '%d', '%s', '%s', '%s', '%s')
                """.formatted(
                webhookMessageId,
                externalId,
                createdDate,
                webhookId,
                eventDate,
                eventType,
                resource,
                resourceExternalId,
                resourceType,
                status)
        ));
    }
    public void addWebhookMessage(int startIdIndex, int recordCount, String externalId, String createdDate, int webhookId, String eventDate, int eventType, String resource, String resourceExternalId, String resourceType, DeliveryStatus status) {
        for (int i = startIdIndex; i <= recordCount; i++) {
            addWebhookMessage(i, i+externalId, createdDate, webhookId, eventDate, eventType, resource, resourceExternalId, resourceType, status);
        }
    }

    public void addWebhookDeliveryQueueMessage(int id, String sentDate, String createdDate, String deliveryResult, int statusCode, int webhookMessageId, DeliveryStatus deliveryStatus, int deliveryCode) {
        jdbi.withHandle(h -> h.execute("""
                INSERT INTO webhook_delivery_queue VALUES
                    ('%d', '%s', '%s', '%s', '%d', '%d', '%s', '%d')
                """.formatted(id, sentDate, createdDate, deliveryResult, statusCode, webhookMessageId, deliveryStatus, deliveryCode)
        ));
    }

    public void truncateAllWebhooksData() {
        jdbi.withHandle(h -> h.createScript(
                "TRUNCATE TABLE webhooks CASCADE; "
        ).execute());
    }
}
