ALTER TABLE webhook_delivery_queue
DROP CONSTRAINT IF EXISTS webhook_delivery_queue_delivery_status_check;

ALTER TABLE webhook_delivery_queue
ADD CONSTRAINT webhook_delivery_queue_delivery_status_check CHECK (delivery_status in ('SUCCESSFUL', 'FAILED', 'PENDING', 'WILL_NOT_SEND'));