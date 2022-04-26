--liquibase formatted sql

--changeset uk.gov.pay:0007_add_index_webhooks_to_send.sql
CREATE INDEX IF NOT EXISTS delivery_status_idx on webhook_delivery_queue(delivery_status)

--rollback DROP INDEX IF EXISTS delivery_status_idx
