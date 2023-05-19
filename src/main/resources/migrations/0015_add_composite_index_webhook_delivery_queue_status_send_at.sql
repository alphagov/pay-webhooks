--liquibase formatted sql

--changeset uk.gov.pay:add-composite-index-webhook-delivery-queue-status-send-at runInTransaction:false
CREATE INDEX CONCURRENTLY IF NOT EXISTS webhook_delivery_queue_send_at_status_idx on webhook_delivery_queue(send_at, delivery_status);
