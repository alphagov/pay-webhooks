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
    

    public void truncateAllData() {
        jdbi.withHandle(h -> h.createScript(
                "TRUNCATE TABLE webhooks CASCADE; "
        ).execute());
    }

    public void createWebhook() {
        jdbi.withHandle(h -> h.createUpdate(
                "INSERT INTO webhooks (created_date, external_id, signing_key, service_id, live, callback_url, status) VALUES ('2011-01-01 00:00:00+09', 'foo', 'bar', 1, true, 'http://example.com', 'ACTIVE');"
        ).execute());
    }

    public void createMessage() {
        jdbi.withHandle(h -> h.createUpdate(
                "INSERT INTO webhook_messages (external_id, created_date, webhook_id, event_date, event_type, resource, resource_external_id, resource_type) VALUES ('foo', '2011-01-01 00:00:00+09', 1, '2011-01-01 00:00:00+09', 1,  '{}', 'bar', 'baz');"
        ).execute());
    }    
    
    public void addMessageToQueue() {
        jdbi.withHandle(h -> h.createUpdate(
                "INSERT INTO webhook_delivery_queue (send_at, created_date, webhook_message_id, delivery_status) VALUES ('2011-01-01 00:00:00+09', '2011-01-01 00:00:00+09', 1, 'PENDING');"
        ).execute());
    }

}
