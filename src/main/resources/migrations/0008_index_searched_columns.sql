CREATE INDEX IF NOT EXISTS webhook_delivery_queue_created_date_idx ON webhook_delivery_queue(created_date);
CREATE INDEX IF NOT EXISTS webhook_delivery_queue_message_id_idx ON webhook_delivery_queue(webhook_message_id);
CREATE INDEX IF NOT EXISTS webhook_delivery_queue_send_at_idx ON webhook_delivery_queue(send_at);

CREATE INDEX IF NOT EXISTS webhook_messages_external_id_idx ON webhook_messages(external_id);
CREATE INDEX IF NOT EXISTS webhook_messages_created_date_idx ON webhook_messages(created_date);
CREATE INDEX IF NOT EXISTS webhook_messages_webhook_id_idx ON webhook_messages(webhook_id);

CREATE INDEX IF NOT EXISTS webhooks_external_id_idx ON webhooks(external_id);
CREATE INDEX IF NOT EXISTS webhooks_created_date_idx ON webhooks(created_date);
CREATE INDEX IF NOT EXISTS webhooks_service_id_idx ON webhooks(service_id);
CREATE INDEX IF NOT EXISTS webhooks_live_idx ON webhooks(live) WHERE live = TRUE;

CREATE UNIQUE INDEX ON webhook_subscriptions(webhook_id, event_type_id);