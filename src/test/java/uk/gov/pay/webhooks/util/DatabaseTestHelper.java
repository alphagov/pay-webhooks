package uk.gov.pay.webhooks.util;

import org.jdbi.v3.core.Jdbi;

public class DatabaseTestHelper {

    private Jdbi jdbi;

    private DatabaseTestHelper(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public static DatabaseTestHelper aDatabaseTestHelper(Jdbi jdbi) {
        return new DatabaseTestHelper(jdbi);
    }

    public void executeSql(String sql) {
        jdbi.withHandle(h -> h.execute(sql));
    }
    
    public void addWebhookWithSubscription(String webhookExternalId, String serviceExternalId, String callbackUrl, String gatewayAccountId) {
        jdbi.withHandle(h -> h.execute("INSERT INTO webhooks VALUES (1, '2022-01-01', '%s', 'signing-key', '%s', false, '%s', 'description', 'ACTIVE', '%s')".formatted(webhookExternalId, serviceExternalId, callbackUrl, gatewayAccountId)));
        jdbi.withHandle(h -> h.execute("INSERT INTO webhook_subscriptions VALUES (1, (SELECT id FROM event_types WHERE name = 'card_payment_succeeded'))"));
        jdbi.withHandle(h -> h.execute("INSERT INTO webhook_subscriptions VALUES (1, (SELECT id FROM event_types WHERE name = 'card_payment_refunded'))"));
    }

    public void addWebhookMessageLastDeliveryStatusIsConsistent(String serviceExternalId, String gatewayAccountId, int wireMockPort) {
        jdbi.withHandle(h -> h.execute("INSERT INTO webhooks VALUES (1, '2022-01-01', 'webhook-external-id-succeeds', 'signing-key', '%s', false, 'http://localhost:%d/a-working-endpoint', 'description', 'ACTIVE', '%s')".formatted(serviceExternalId, wireMockPort, gatewayAccountId)));
        jdbi.withHandle(h -> h.execute("INSERT INTO webhooks VALUES (2, '2022-01-01', 'webhook-external-id-fails', 'signing-key', '%s', false, 'http://localhost:%d/a-failing-endpoint', 'description', 'ACTIVE', '%s')".formatted(serviceExternalId, wireMockPort, gatewayAccountId)));
        jdbi.withHandle(h -> h.execute("INSERT INTO webhook_subscriptions VALUES (1, (SELECT id FROM event_types WHERE name = 'card_payment_succeeded'))"));
        jdbi.withHandle(h -> h.execute("INSERT INTO webhook_subscriptions VALUES (2, (SELECT id FROM event_types WHERE name = 'card_payment_succeeded'))"));
    }

    public void addDeliveryStatusEnumIsConsistentWithDatabase() {
        jdbi.withHandle(h -> h.execute("INSERT INTO webhooks VALUES (1, '2022-01-01', 'webhook-external-id', 'signing-key', 'service-id', true, 'https://callback-url.test', 'description', 'ACTIVE')"));
        jdbi.withHandle(h -> h.execute("INSERT INTO webhook_messages VALUES (1, 'message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', 'transaction-external-id', 'payment')"));
    }
    
    public void truncateAllData() {
        jdbi.withHandle(h -> h.createScript(
                "TRUNCATE TABLE webhooks CASCADE; "
        ).execute());
    }
    
}
