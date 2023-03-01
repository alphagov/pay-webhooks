ALTER TABLE webhook_messages
ADD COLUMN last_delivery_status VARCHAR(64) check (last_delivery_status in ('SUCCESSFUL', 'FAILED', 'PENDING', 'WILL_NOT_SEND'));

CREATE INDEX IF NOT EXISTS last_delivery_status_idx on webhook_messages(last_delivery_status)