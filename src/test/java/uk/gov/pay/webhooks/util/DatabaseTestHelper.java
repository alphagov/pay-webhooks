package uk.gov.pay.webhooks.util;

import org.jdbi.v3.core.Jdbi;
import uk.gov.pay.webhooks.deliveryqueue.DeliveryStatus;

import java.util.List;

public class DatabaseTestHelper {

    public static final String EVENT_TYPE_SELECT_QUERY_CARD_PAYMENT_SUCCEEDED = "SELECT id FROM event_types WHERE name = 'card_payment_succeeded'";
    public static final String EVENT_TYPE_SELECT_QUERY_CARD_PAYMENT_REFUNDED = "SELECT id FROM event_types WHERE name = 'card_payment_refunded'";
    private Jdbi jdbi;

    private DatabaseTestHelper(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public static DatabaseTestHelper aDatabaseTestHelper(Jdbi jdbi) {
        return new DatabaseTestHelper(jdbi);
    }
    
    public void addWebhookWithSubscription(String webhookExternalId, String serviceExternalId, String callbackUrl, String gatewayAccountId) {
        jdbi.withHandle(h -> h.execute("INSERT INTO webhooks VALUES (1, '2022-01-01', '%s', 'signing-key', '%s', false, '%s', 'description', 'ACTIVE', '%s')".formatted(webhookExternalId, serviceExternalId, callbackUrl, gatewayAccountId)));
        jdbi.withHandle(h -> h.execute("INSERT INTO webhook_subscriptions VALUES (1, (" + EVENT_TYPE_SELECT_QUERY_CARD_PAYMENT_SUCCEEDED + "))"));
        jdbi.withHandle(h -> h.execute("INSERT INTO webhook_subscriptions VALUES (1, (" + EVENT_TYPE_SELECT_QUERY_CARD_PAYMENT_REFUNDED + "))"));
    }

    public void addWebhookMessageLastDeliveryStatusIsConsistent(String serviceExternalId, String gatewayAccountId, int wireMockPort) {
        jdbi.withHandle(h -> h.execute("INSERT INTO webhooks VALUES (1, '2022-01-01', 'webhook-external-id-succeeds', 'signing-key', '%s', false, 'http://localhost:%d/a-working-endpoint', 'description', 'ACTIVE', '%s')".formatted(serviceExternalId, wireMockPort, gatewayAccountId)));
        jdbi.withHandle(h -> h.execute("INSERT INTO webhooks VALUES (2, '2022-01-01', 'webhook-external-id-fails', 'signing-key', '%s', false, 'http://localhost:%d/a-failing-endpoint', 'description', 'ACTIVE', '%s')".formatted(serviceExternalId, wireMockPort, gatewayAccountId)));
        jdbi.withHandle(h -> h.execute("INSERT INTO webhook_subscriptions VALUES (1, (" + EVENT_TYPE_SELECT_QUERY_CARD_PAYMENT_SUCCEEDED + "))"));
        jdbi.withHandle(h -> h.execute("INSERT INTO webhook_subscriptions VALUES (2, (" + EVENT_TYPE_SELECT_QUERY_CARD_PAYMENT_SUCCEEDED + "))"));
    }

    public void addDeliveryStatusEnumIsConsistentWithDatabase() {
        jdbi.withHandle(h -> h.execute("INSERT INTO webhooks VALUES (1, '2022-01-01', 'webhook-external-id', 'signing-key', 'service-id', true, 'https://callback-url.test', 'description', 'ACTIVE')"));
        jdbi.withHandle(h -> h.execute("INSERT INTO webhook_messages VALUES (1, 'message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', 'transaction-external-id', 'payment')"));
    }

    public void addDeliveryStatusEnumIsConsistentWithDatabaseWebHookDeliveryQueueInsert(DeliveryStatus status) {
        jdbi.withHandle(h -> h.execute("INSERT INTO webhook_delivery_queue VALUES (1, '2022-01-01', '2022-01-01', '200', 200, 1, '%s', 1250)".formatted(status)));
    }

    public void deliveryStatusEnumIsConsistentWithWebhookMessageLastDeliveryStatusWebHooksInsert() {
        jdbi.withHandle(h -> h.execute("INSERT INTO webhooks VALUES (1, '2022-01-01', 'webhook-external-id', 'signing-key', 'service-id', true, 'https://callback-url.test', 'description', 'ACTIVE')"));
    }

    public void deliveryStatusEnumIsConsistentWithWebhookMessageLastDeliveryStatusMessagesInsert(DeliveryStatus status) {
        jdbi.withHandle(h -> h.execute("INSERT INTO webhook_messages VALUES (1, 'message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', 'transaction-external-id', 'payment', '%s')".formatted(status)));
    }

    public void truncateAllData() {
        jdbi.withHandle(h -> h.createScript(
                "TRUNCATE TABLE webhooks CASCADE; "
        ).execute());
    }

    public void shouldReturnAndCountEmptyMessagesWebhooksInsert(String externalId) {
        jdbi.withHandle(h -> h.execute("INSERT INTO webhooks VALUES (1, '2022-01-01', '%s', 'signing-key', 'service-id', true, 'http://callback-url.com', 'description', 'ACTIVE')".formatted(externalId)));
    }

    public void setupThreeWebhookMessagesThatShouldNotBeDeletedWebhookMessagesInsert(List<String> webhookMessageExternalIds, String date) {
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

    public void setupThreeWebhookMessagesThatShouldNotBeDeletedWebhookDeliveryQueueInsert(String date) {
        jdbi.withHandle(h -> h.execute("""
                INSERT INTO webhook_delivery_queue VALUES
                    (15, '%s', '%s', '200', 200, 13, 'SUCCESSFUL', 1250),
                    (16, '%s', '%s', '404', 404, 14, 'FAILED', 25),
                    (17, '%s', '%s', null, null, 15, 'PENDING', null)
                """.formatted(date, date, date, date, date, date)
        ));
    }

    public void setupWebhookWithMessagesExpectedToBePartiallyDeletedWebhooksInsert(String externalId) {
        jdbi.withHandle(h -> h.execute(
                "INSERT INTO webhooks VALUES (1, '2022-01-01', '%s', 'signing-key', 'service-id', true, 'http://callback-url.com', 'description', 'ACTIVE')".formatted(externalId)
        ));
    }

    public void setupWebhookWithMessagesExpectedToBePartiallyDeletedWebhookMessagesInsert() {
        jdbi.withHandle(h -> h.execute("""
                    INSERT INTO webhook_messages VALUES
                    (1, 'first-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', 'transaction-external-id', 'payment', 'FAILED'),
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

    public void setupWebhookWithMessagesExpectedToBePartiallyDeletedWebhookDeliveryQueueInsert() {
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

    public void setupWebhookWithMessagesWebHooksInsert(String externalId) {
        jdbi.withHandle(h -> h.execute(
                "INSERT INTO webhooks VALUES (1, '2022-01-01', '%s', 'signing-key', 'service-id', true, 'http://callback-url.com', 'description', 'ACTIVE', '100')".formatted(externalId)
        ));
    }

    public void setupWebhookWithMessagesWebhookMessagesInsert(String messageExternalId) {
        jdbi.withHandle(h -> h.execute("""
                    INSERT INTO webhook_messages VALUES
                    (1, '%s', '2022-01-01', 1, '2022-01-01', 1, '{}', 'transaction-external-id', 'payment', 'FAILED'),
                    (2, 'second-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', 'transaction-external-id-2', 'payment', 'FAILED'),
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
                """.formatted(messageExternalId)
        ));
    }

    public void setupWebhookWithMessagesWebhookDeliveryQueueInsert() {
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
}
