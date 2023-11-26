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

    public void addWebhookMessage(List<String> webhookMessageExternalIds, String date) {
        jdbi.withHandle(h -> h.execute("""
                    INSERT INTO webhook_messages VALUES
                    (13, '%s', '%s', 1, '%s', 1, '{}', 'transaction-external-id', 'payment', 'FAILED'),
                    (14, '%s', '%s', 1, '%s', 1, '{}', null, null, null),
                    (15, '%s', '%s', 1, '%s', 1, '{}', null, null, null)
                """.formatted(
                webhookMessageExternalIds.get(0), date, date,
                webhookMessageExternalIds.get(1), date, date,
                webhookMessageExternalIds.get(2), date, date)
        ));
    }

    public void addWebhookMessages(int startIdIndex, int recordCount) {
        for (int i = startIdIndex; i <= recordCount; i++) {
            addWebhookMessage(i + 1, (i + 1) + "-message-external-id", "2022-01-01", 1, "2022-01-01", 1, "{}", null, null, null);
        }
    }

    public void webhookMessageLastDeliveryStatusIsConsistent(){
        jdbi.withHandle(h -> h.execute("INSERT INTO webhooks VALUES (1, '2022-01-01', 'webhook-external-id-succeeds', 'signing-key', '%s', false, 'http://localhost:%d/a-working-endpoint', 'description', 'ACTIVE', '%s')".formatted(serviceExternalId, app.getWireMockPort(), gatewayAccountId)));
        jdbi.withHandle(h -> h.execute("INSERT INTO webhooks VALUES (2, '2022-01-01', 'webhook-external-id-fails', 'signing-key', '%s', false, 'http://localhost:%d/a-failing-endpoint', 'description', 'ACTIVE', '%s')".formatted(serviceExternalId, app.getWireMockPort(), gatewayAccountId)));
        jdbi.withHandle(h -> h.execute("INSERT INTO webhook_subscriptions VALUES (1, (SELECT id FROM event_types WHERE name = 'card_payment_succeeded'))"));
        jdbi.withHandle(h -> h.execute("INSERT INTO webhook_subscriptions VALUES (2, (SELECT id FROM event_types WHERE name = 'card_payment_succeeded'))"));

}
    public void addThreeWebhookMessagesThatShouldNotBeDeleted(String date) {
        jdbi.withHandle(h -> h.execute("""
                INSERT INTO webhook_delivery_queue VALUES
                    (15, '%s', '%s', '200', 200, 13, 'SUCCESSFUL', 1250),
                    (16, '%s', '%s', '404', 404, 14, 'FAILED', 25),
                    (17, '%s', '%s', null, null, 15, 'PENDING', null)
                """.

                formatted(date, date, date, date, date, date)
        ));
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

    public void addWebhookDeliveryQueueMessage(int id, String sentDate, String createdDate, String deliveryResult, int statusCode, int webhookMessageId, DeliveryStatus deliveryStatus, String deliveryCode) {
        jdbi.withHandle(h -> h.execute("""
                INSERT INTO webhook_delivery_queue VALUES
                    ('%d', '%s', '%s', '%s', '%d', '%d', '%s', '%d')
                """.formatted(id, sentDate, createdDate, deliveryResult, statusCode, webhookMessageId, deliveryStatus, deliveryCode)
        ));
    }


    public void addWebhookDeliveryQueueWithMessagesExpectedToBePartiallyDeleted() {
        jdbi.withHandle(h -> h.execute("""
                INSERT INTO webhook_delivery_queue VALUES
                    (1, '2022-01-01', '2022-01-01', '200', 200, 1, 'SUCCESSFUL', 1250),
                    (2, '2022-01-02', '2022-01-01', '404', 404, 1, 'FAILED', 25),
                    (3, '2022-01-02', '2022-01-01', null, null, 1, 'PENDING', null),
                    (4, '2022-01-01', '2022-01-01', '404', 404, 2, 'PENDING', null),
                    (5, '2022-01-01', '2022-01-01', '404', 404, 3, 'PENDING', null),
                    (6, '2022-01-01', '2022-01-01', '404', 404, 4, 'PENDING', null),
                    (7, '2022-01-01', '2022-01-01', '404', 404, 5, 'PENDING', null),
                    (8, '2022-01-01', '2022-01-01', '404', 404, 6, 'PENDING', null),
                    (9, '2022-01-01', '2022-01-01', '404', 404, 7, 'PENDING', null),
                    (10, '2022-01-01', '2022-01-01', '404', 404, 8, 'PENDING', null),
                    (11, '2022-01-01', '2022-01-01', '404', 404, 9, 'PENDING', null),
                    (12, '2022-01-01', '2022-01-01', '404', 404, 10, 'PENDING', null),
                    (13, '2022-01-01', '2022-01-01', '404', 404, 11, 'PENDING', null)
                """
        ));
    }

    public void addWebhookDeliveryQueueWithMessages() {
        jdbi.withHandle(h -> h.execute("""
                INSERT INTO webhook_delivery_queue VALUES
                    (1, '2022-01-01', '2022-01-01', '200', 200, 1, 'SUCCESSFUL', 1250),
                    (2, '2022-01-02', '2022-01-01', '404', 404, 1, 'FAILED', 25),
                    (3, '2022-01-02', '2022-01-01', null, null, 1, 'PENDING', null),
                    (4, '2022-01-01', '2022-01-01', '404', 404, 2, 'PENDING', null),
                    (5, '2022-01-01', '2022-01-01', '404', 404, 3, 'PENDING', null),
                    (6, '2022-01-01', '2022-01-01', '404', 404, 4, 'PENDING', null),
                    (7, '2022-01-01', '2022-01-01', '404', 404, 5, 'PENDING', null),
                    (8, '2022-01-01', '2022-01-01', '404', 404, 6, 'PENDING', null),
                    (9, '2022-01-01', '2022-01-01', '404', 404, 7, 'PENDING', null),
                    (10, '2022-01-01', '2022-01-01', '404', 404, 8, 'PENDING', null),
                    (11, '2022-01-01', '2022-01-01', '404', 404, 9, 'PENDING', null),
                    (12, '2022-01-01', '2022-01-01', '404', 404, 10, 'PENDING', null),
                    (13, '2022-01-01', '2022-01-01', '404', 404, 11, 'PENDING', null),
                    (14, '2022-01-01', '2022-01-01', '404', 404, 12, 'PENDING', null)
                """
        ));
    }

    public void truncateAllWebhooksData() {
        jdbi.withHandle(h -> h.createScript(
                "TRUNCATE TABLE webhooks CASCADE; "
        ).execute());
    }
}
