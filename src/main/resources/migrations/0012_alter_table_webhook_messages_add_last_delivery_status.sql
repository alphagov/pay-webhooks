--liquibase formatted sql

--changeset uk.gov.pay:alter-table-webhook-messages-add-last-delivery-status
ALTER TABLE webhook_messages
ADD COLUMN last_delivery_status VARCHAR(64) check (last_delivery_status in ('SUCCESSFUL', 'FAILED', 'PENDING', 'WILL_NOT_SEND'));

--changeset uk.gov.pay:alter-table-webhook-messages-index-last-delivery-status runInTransaction:false
CREATE INDEX CONCURRENTLY IF NOT EXISTS last_delivery_status_idx on webhook_messages(last_delivery_status)