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

    public void addWebhook(int webhookId, String webhookExternalId, String serviceExternalId, int wireMockPort, String gatewayAccountId) {
        jdbi.withHandle(h -> h.execute("""
                    INSERT INTO webhooks VALUES
                    ('%d', '2022-01-01', '%s', 'signing-key', '%s', false, 'http://localhost:%d/a-working-endpoint', 'description', 'ACTIVE', '%s')
                """.formatted(
                webhookId, webhookExternalId, serviceExternalId, wireMockPort, gatewayAccountId)
        ));
    }

    public void addWebhookSubscription(int webhookSubscriptionId, String event) {
        jdbi.withHandle(h -> h.execute("INSERT INTO webhook_subscriptions VALUES ('%d', (SELECT id FROM event_types WHERE name = '%s'))".formatted(webhookSubscriptionId, event)));
    }

    public void addWebhookDeliveryStatusEnumIsConsistentWithDatabase() {
        jdbi.withHandle(h -> h.execute("INSERT INTO webhook_messages VALUES (1, 'message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', 'transaction-external-id', 'payment')"));
    }

    public void addWebhookMessage(int webhookMessageId, String externalId, String createdDate, int webhookId, String eventDate, int eventType, String resource, String resourceExternalId, String resourceType, String status) {
        jdbi.withHandle(h -> h.execute("INSERT INTO webhook_messages VALUES ('%d', '%s', '%s', '%d', '%s', '%d', '%s', '%s', '%s', '%s')".
                formatted(webhookMessageId, externalId, createdDate, webhookId, eventDate, eventType, resource, resourceExternalId, resourceType, status)));
    }

    public void addWebhookMessages(int startIdIndex, int recordCount) {
        for (int i = startIdIndex; i <= recordCount; i++) {
            addWebhookMessage(i + 1, (i + 1) + "-message-external-id", "2022-01-01", 1, "2022-01-01", 1, "{}", null, null, null);
        }
    }

    public void addWebhookMessagesExpectedToBePartiallyDeleted() {
        jdbi.withHandle(h -> h.execute("""
                    INSERT INTO webhook_messages VALUES
                    (2, 'second-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                    (3, 'third-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                    (4, 'fourth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                    (5, 'fifth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                    (6, 'sixth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                    (7, 'seventh-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                    (8, 'eighth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                    (9, 'ninth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                    (10, 'tenth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                    (11, 'eleventh-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null)
                """
        ));
    }

    public void addWebhookMessages() {
        jdbi.withHandle(h -> h.execute("""
                    INSERT INTO webhook_messages VALUES
                    (3, 'third-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                    (4, 'fourth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                    (5, 'fifth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                    (6, 'sixth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                    (7, 'seventh-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                    (8, 'eighth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                    (9, 'ninth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                    (10, 'tenth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                    (11, 'eleventh-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                    (12, 'twelfth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null)
                """
        ));
    }

    public void addWebhookDeliveryQueueStatusEnumIsConsistentWithDatabase(DeliveryStatus status) {
        jdbi.withHandle(h -> h.execute("INSERT INTO webhook_delivery_queue VALUES (1, '2022-01-01', '2022-01-01', '200', 200, 1, '%s', 1250)".formatted(status)));
    }

    public void addWebhookDeliveryQueueMessage(int id, String sentDate, String createdDate, String deliveryResult, String statusCode, int webhookMessageId, String deliveryStatus, String deliveryCode) {
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
