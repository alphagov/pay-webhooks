ALTER TABLE webhook_delivery_queue
DROP CONSTRAINT fk_webhook_message_id,
ADD CONSTRAINT fk_webhook_message_id FOREIGN KEY (webhook_message_id) REFERENCES webhook_messages (id) ON DELETE CASCADE;