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
    
    public void addWebhookWithSubscription(String webhookExternalId, String serviceExternalId, String callbackUrl) {
        jdbi.withHandle(h -> h.execute("INSERT INTO webhooks VALUES (1, '2022-01-01', '%s', 'signing-key', '%s', false, '%s', 'description', 'ACTIVE')".formatted(webhookExternalId, serviceExternalId, callbackUrl)));
        jdbi.withHandle(h -> h.execute("INSERT INTO webhook_subscriptions VALUES (1, (SELECT id FROM event_types WHERE name = 'card_payment_succeeded'))"));
    }

    public void truncateAllData() {
        jdbi.withHandle(h -> h.createScript(
                "TRUNCATE TABLE webhooks CASCADE; "
        ).execute());
    }
    
}
