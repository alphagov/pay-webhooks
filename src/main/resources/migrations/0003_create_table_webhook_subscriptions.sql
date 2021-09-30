--liquibase formatted sql

--changeset uk.gov.pay:create-table-webhook_subscriptions

CREATE table webhook_subscriptions (
    webhook_id INT,
    event_type_id INT
);

ALTER TABLE webhook_subscriptions ADD CONSTRAINT fk_webhook_id FOREIGN KEY (webhook_id) REFERENCES webhooks (id) ON DELETE CASCADE;
ALTER TABLE webhook_subscriptions ADD CONSTRAINT fk_event_type_id FOREIGN KEY (event_type_id) REFERENCES event_types (id) ON DELETE CASCADE;

--rollback DROP table webhook_subscriptions
